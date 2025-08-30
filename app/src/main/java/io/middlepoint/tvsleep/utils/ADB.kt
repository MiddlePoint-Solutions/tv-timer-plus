package io.middlepoint.tvsleep.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import co.touchlab.kermit.Logger
import com.draco.ladb.utils.DnsDiscover
import io.middlepoint.tvsleep.AdbState
import io.middlepoint.tvsleep.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class ADB(
    private val context: Context,
) {
    private val logger = Logger.withTag("ADB")
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val adbPath = "${context.applicationInfo.nativeLibraryDir}/libadb.so"
    private val scriptPath = "${context.getExternalFilesDir(null)}/script.sh"

    val appUsageForegroundWatcher =
        AppUsageForegroundWatcher(context) { pkg ->
            logger.d("Foreground: $pkg")
            if (pkg == "com.android.tv.settings") {
                debug("Settings app detected in foreground. Force-stopping...")
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.IO).launch { // Or use a pre-existing scope if available
                    sendToShellProcess("am force-stop com.android.tv.settings")
                }
            }
        }

    /**
     * Is the shell ready to handle commands?
     */
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private var tryingToPair = false

    /**
     * Is the shell closed for any reason?
     */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed

    /**
     * State of the ADB connection
     */
    private val _state = MutableStateFlow<AdbState>(AdbState.Idle)
    val state: StateFlow<AdbState> = _state

    /**
     * Where shell output is stored
     */
    val outputBufferFile: File =
        File.createTempFile("buffer", ".txt").also {
            it.deleteOnExit()
        }

    /**
     * Single shell instance where we can pipe commands to
     */
    private var shellProcess: Process? = null

    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private fun startAdbSysdumpMonitoring() {
        if (monitorJob?.isActive == true) {
            debug("Monitor is already running.")
            return
        }
        monitorJob =
            monitorScope.launch {
                debug("Window monitor coroutine started.")
                while (isActive) {
                    try {
                        val output =
                            withContext(Dispatchers.IO) {
                                val checkProcess =
                                    adb(false, listOf("shell", "dumpsys window windows | grep mCurrentFocus"))
                                val reader = BufferedReader(checkProcess.inputStream.reader())
                                val line = reader.readLine()
                                checkProcess.waitFor()
                                line
                            }

                        if (output != null && output.contains("com.android.tv.settings")) {
                            debug("Settings app detected in foreground. Force-stopping...")
                            withContext(Dispatchers.IO) {
                                val stopProcess =
                                    adb(false, listOf("shell", "am", "force-stop", "com.android.tv.settings"))
                                stopProcess.waitFor()
                            }
                        }

                        delay(250)
                    } catch (e: CancellationException) {
                        debug("Window monitor coroutine cancelled.")
                        break // Exit loop on cancellation
                    } catch (e: Exception) {
                        debug("An error occurred in the window monitor coroutine: ${e.message}")
                        delay(1000) // Delay to prevent rapid-fire error loops
                    }
                }
                debug("Window monitor coroutine stopped.")
            }
    }

    fun stopMonitoring() {
        if (monitorJob?.isActive != true) {
            debug("Monitor is not running.")
            return
        }
        monitorJob?.cancel()
        monitorJob = null
        debug("Requested to stop window monitor.")
    }

    /**
     * Returns the user buffer size if valid, else the default
     */
    fun getOutputBufferSize(): Int {
//        val userValue = sharedPrefs.getString(context.getString(R.string.buffer_size_key), "16384")!!
        val userValue = "16384"
        return try {
            Integer.parseInt(userValue)
        } catch (_: NumberFormatException) {
            MAX_OUTPUT_BUFFER_SIZE
        }
    }

    /**
     * Get a list of connected devices.
     */
    suspend fun getDevices(): List<String> {
        val devicesProcess = adb(false, listOf("devices"))
        withContext(Dispatchers.IO) {
            devicesProcess.waitFor()
        }

        val linesRaw = BufferedReader(devicesProcess.inputStream.reader()).readLines()
        val deviceLines = linesRaw.filterNot { it.contains("List of devices attached") }
        var deviceNames = deviceLines.map { it.split("	").first() }
        deviceNames = deviceNames.filterNot { it.isEmpty() }

        for (name in deviceNames) {
            Log.d("LINES", "<<<$name>>>")
        }

        return deviceNames
    }

    /**
     * Start the ADB server
     */
    suspend fun initServer(): Boolean {
        if (_running.value || tryingToPair) {
            return true
        }

        tryingToPair = true
        val autoShell = true
        val secureSettingsGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        _state.value = AdbState.Connecting

        if (autoShell) {
            if (secureSettingsGranted) {
                disableMobileDataAlwaysOn()
                if (!isUSBDebuggingEnabled()) {
                    debug("Turning on USB debugging...")
                    withContext(Dispatchers.IO) {
                        Settings.Global.putInt(
                            context.contentResolver,
                            Settings.Global.ADB_ENABLED,
                            1,
                        )
                    }
                    delay(5_000)
                }
            }

            if (!isUSBDebuggingEnabled()) {
                _state.value = AdbState.Failed("USB debugging is not enabled!")
                debug("USB debugging is not enabled!")
                debug("Settings -> Developer options -> USB debugging")
                debug("Waiting for USB debugging...")
                while (!isUSBDebuggingEnabled()) {
                    delay(1_000)
                }
            }

            val nowTimeInitial = System.currentTimeMillis()
            val maxTimeoutTime = nowTimeInitial + 10.seconds.inWholeMilliseconds
            val minDnsScanTime =
                (DnsDiscover.Companion.aliveTime ?: nowTimeInitial) + 3.seconds.inWholeMilliseconds
            while (true) {
                val currentTime = System.currentTimeMillis()
                val pendingResolves = DnsDiscover.Companion.pendingResolves.get()
                if (currentTime >= minDnsScanTime && !pendingResolves) {
                    debug("DNS resolver done...")
                    break
                }
                if (currentTime >= maxTimeoutTime) {
                    debug("DNS resolver took too long! Skipping...")
                    break
                }
                debug("Awaiting DNS resolver...")
                delay(1_000)
            }

            val adbPort = DnsDiscover.Companion.adbPort
            if (adbPort != null) {
                debug("Best ADB port discovered: $adbPort")
            } else {
                debug("No ADB port discovered, fallback...")
            }

            debug("Starting ADB server...")
            withContext(Dispatchers.IO) {
                adb(false, listOf("start-server")).waitFor(1, TimeUnit.MINUTES)
            }

            val waitProcessResult =
                withContext(Dispatchers.IO) {
                    if (adbPort != null) {
                        adb(false, listOf("connect", "localhost:$adbPort")).waitFor(1, TimeUnit.MINUTES)
                    } else {
                        adb(false, listOf("wait-for-device")).waitFor(1, TimeUnit.MINUTES)
                    }
                }

            if (!waitProcessResult) {
                debug("Your device didn't connect to LADB")
                debug("If a reboot doesn't work, please contact support")
                if (isMobileDataAlwaysOnEnabled()) {
                    debug("Please disable 'Mobile data always on' in Developer Settings!")
                    delay(5_000)
                }
                tryingToPair = false
                return false
            }
        }

        val deviceList = getDevices()
        Log.d("DEVICES", "Devices: $deviceList")

        shellProcess =
            if (autoShell) {
                var argList = listOf("shell")
                if (deviceList.size > 1) {
                    Log.w("DEVICES", "Multiple devices detected...")
                    val localDevices = deviceList.filter { it.contains("localhost") }
                    if (localDevices.isNotEmpty()) {
                        val serialId = localDevices.first()
                        Log.w("DEVICES", "Choosing first local device: $serialId")
                        argList = listOf("-s", serialId, "shell")
                    } else {
                        val nonEmulators = deviceList.filterNot { it.contains("emulator") }
                        if (nonEmulators.isNotEmpty()) {
                            val serialId = nonEmulators.first()
                            Log.w("DEVICES", "Choosing first non-emulator device: $serialId")
                            argList = listOf("-s", serialId, "shell")
                        } else {
                            val serialId = deviceList.first()
                            Log.w("DEVICES", "Choosing first unrecognized device: $serialId")
                            argList = listOf("-s", serialId, "shell")
                        }
                    }
                }
                adb(true, argList)
            } else {
                shell(true, listOf("sh", "-l"))
            }

        sendToShellProcess("alias adb=\"$adbPath\"")

        if (!secureSettingsGranted) {
            sendToShellProcess("pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS &> /dev/null")
        }

        if (autoShell) {
            sendToShellProcess("echo 'Entered adb shell'")
        } else {
            sendToShellProcess("echo 'Entered non-adb shell'")
        }

        val startupCommand = "echo 'Success! ※\\(^o^)/※'"
        if (startupCommand.isNotEmpty()) {
            sendToShellProcess(startupCommand)
        }

        _state.value = AdbState.Ready
        _running.value = true
        tryingToPair = false
        return true
    }

    private fun isWirelessDebuggingEnabled() = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
    private fun isUSBDebuggingEnabled() = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    private fun isMobileDataAlwaysOnEnabled() = Settings.Global.getInt(context.contentResolver, "mobile_data_always_on", 0) == 1

    suspend fun disableMobileDataAlwaysOn() {
        val secureSettingsGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        if (secureSettingsGranted) {
            if (isMobileDataAlwaysOnEnabled()) {
                debug("Disabling 'Mobile data always on'...")
                withContext(Dispatchers.IO) {
                    Settings.Global.putInt(
                        context.contentResolver,
                        "mobile_data_always_on",
                        0,
                    )
                }
                delay(3_000)
            }
        }
    }

    suspend fun cycleWirelessDebugging() {
        val secureSettingsGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        if (secureSettingsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                debug("Cycling wireless debugging, please wait...")
                if (isWirelessDebuggingEnabled()) {
                    debug("Turning off wireless debugging...")
                    withContext(Dispatchers.IO) {
                        Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 0)
                    }
                    delay(3_000)
                }
                debug("Turning on wireless debugging...")
                withContext(Dispatchers.IO) {
                    Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
                }
                delay(3_000)
                debug("Turning off wireless debugging...")
                withContext(Dispatchers.IO) {
                    Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 0)
                }
                delay(3_000)
                debug("Turning on wireless debugging...")
                withContext(Dispatchers.IO) {
                    Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
                }
                delay(3_000)
            }
        }
    }

    suspend fun waitForDeathAndReset() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                if (tryingToPair) {
                    delay(100)
                    continue
                }
                shellProcess?.waitFor()
                _running.value = false
                debug("Shell is dead, resetting...")
                adb(false, listOf("kill-server")).waitFor()
                delay(3_000)
                initServer()
            }
        }

    suspend fun pair(
        port: String,
        pairingCode: String,
    ): Boolean {
        val pairShell = adb(false, listOf("pair", "localhost:$port"))
        delay(5000)
        withContext(Dispatchers.IO) {
            PrintStream(pairShell.outputStream).apply {
                println(pairingCode)
                flush()
            }
        }
        val exitCode =
            withContext(Dispatchers.IO) {
                pairShell.waitFor(10, TimeUnit.SECONDS)
                val exited = pairShell.exitValue()
                pairShell.destroyForcibly().waitFor()
                if (exited != null) exited else -1
            }
        val killShell = adb(false, listOf("kill-server"))
        withContext(Dispatchers.IO) {
            killShell.waitFor(3, TimeUnit.SECONDS)
            killShell.destroyForcibly().waitFor()
        }
        return exitCode == 0
    }

    private fun adb(
        redirect: Boolean,
        command: List<String>,
    ): Process {
        val commandList = command.toMutableList().also { it.add(0, adbPath) }
        return shell(redirect, commandList)
    }

    private fun shell(
        redirect: Boolean,
        command: List<String>,
    ): Process {
        val processBuilder =
            ProcessBuilder(command)
                .directory(context.filesDir)
                .apply {
                    if (redirect) {
                        redirectErrorStream(true)
                        redirectOutput(outputBufferFile)
                    }
                    environment().apply {
                        put("HOME", context.filesDir.path)
                        put("TMPDIR", context.cacheDir.path)
                    }
                }
        return processBuilder.start()!!
    }

    suspend fun sendToShellProcess(msg: String) {
        if (shellProcess == null || shellProcess?.outputStream == null) {
            return
        }
        withContext(Dispatchers.IO) {
            PrintStream(shellProcess!!.outputStream!!).apply {
                println(msg)
                flush()
            }
        }
    }

    fun debug(msg: String) {
        synchronized(outputBufferFile) {
            Log.d("DEBUG", msg)
            if (outputBufferFile.exists()) {
                outputBufferFile.appendText("* $msg" + System.lineSeparator())
            }
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    companion object {
        const val MAX_OUTPUT_BUFFER_SIZE = 1024 * 16
        const val OUTPUT_BUFFER_DELAY_MS = 100L

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ADB? = null

        fun getInstance(context: Context): ADB =
            instance ?: synchronized(this) {
                instance ?: ADB(context).also { instance = it }
            }
    }
}
