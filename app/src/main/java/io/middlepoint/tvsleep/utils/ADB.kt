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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.touchlab.kermit.Logger
import com.draco.ladb.utils.DnsDiscover
import io.middlepoint.tvsleep.BuildConfig
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class ADB(private val context: Context) {

  private val logger = Logger.withTag("ADB")
  private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

  private val adbPath = "${context.applicationInfo.nativeLibraryDir}/libadb.so"
  private val scriptPath = "${context.getExternalFilesDir(null)}/script.sh"

  val appUsageForegroundWatcher = AppUsageForegroundWatcher(context) { pkg ->
    // e.g., update UI or log
    logger.d("Foreground: $pkg")
  }

  /**
   * Is the shell ready to handle commands?
   */
  private val _running = MutableLiveData(false)
  val running: LiveData<Boolean> = _running

  private var tryingToPair = false

  /**
   * Is the shell closed for any reason?
   */
  private val _closed = MutableLiveData(false)
  val closed: LiveData<Boolean> = _closed

  /**
   * Where shell output is stored
   */
  val outputBufferFile: File = File.createTempFile("buffer", ".txt").also {
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
    monitorJob = monitorScope.launch {
      debug("Window monitor coroutine started.")
      while (isActive) {
        try {
          val output = withContext(Dispatchers.IO) {
            val checkProcess = adb(false, listOf("shell", "dumpsys window windows | grep mCurrentFocus"))
            val reader = BufferedReader(checkProcess.inputStream.reader())
            val line = reader.readLine()
            checkProcess.waitFor()
            line
          }

          if (output != null && output.contains("com.android.tv.settings")) {
            debug("Settings app detected in foreground. Force-stopping...")
            withContext(Dispatchers.IO) {
              val stopProcess = adb(false, listOf("shell", "am", "force-stop", "com.android.tv.settings"))
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
  fun getDevices(): List<String> {
    val devicesProcess = adb(false, listOf("devices"))
    devicesProcess.waitFor()

    /* Get result of the command. */
    val linesRaw = BufferedReader(devicesProcess.inputStream.reader()).readLines()

    /* Remove "List of devices attached" line if it exists (it should). */
    val deviceLines = linesRaw.filterNot { it ->
      it.contains("List of devices attached")
    }

    /* Just get first part with device name/IP and port. */
    var deviceNames = deviceLines.map { it ->
      it.split("\t").first()
    }

    /* Remove any empty lines. */
    deviceNames = deviceNames.filterNot { it ->
      it.isEmpty()
    }

    for (name in deviceNames) {
      Log.d("LINES", "<<<$name>>>")
    }

    return deviceNames
  }

  /**
   * Start the ADB server
   */
  fun initServer(): Boolean {
    if (_running.value == true || tryingToPair)
      return true

    tryingToPair = true

//        val autoShell = sharedPrefs.getBoolean(context.getString(R.string.auto_shell_key), true)
    val autoShell = true

    val secureSettingsGranted =
      context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    if (autoShell) {
      /* Only do wireless debugging steps on compatible versions */
      if (secureSettingsGranted) {
        disableMobileDataAlwaysOn()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    cycleWirelessDebugging()
//                } else

        if (!isUSBDebuggingEnabled()) {
          debug("Turning on USB debugging...")
          Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            1
          )

          Thread.sleep(5_000)
        }
      }

      /* Check again... */
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if (!isWirelessDebuggingEnabled()) {
//                    debug("Wireless debugging is not enabled!")
//                    debug("Settings -> Developer options -> Wireless debugging")
//                    debug("Waiting for wireless debugging...")
//
//                    while (!isWirelessDebuggingEnabled()) {
//                        Thread.sleep(1_000)
//                    }
//                }
//            } else {
      if (!isUSBDebuggingEnabled()) {
        debug("USB debugging is not enabled!")
        debug("Settings -> Developer options -> USB debugging")
        debug("Waiting for USB debugging...")

        while (!isUSBDebuggingEnabled()) {
          Thread.sleep(1_000)
        }
      }
//            }

      val nowTime = System.currentTimeMillis()
      val maxTimeoutTime = nowTime + 10.seconds.inWholeMilliseconds
      val minDnsScanTime = (DnsDiscover.Companion.aliveTime ?: nowTime) + 3.seconds.inWholeMilliseconds
      while (true) {
        val nowTime = System.currentTimeMillis()
        val pendingResolves = DnsDiscover.Companion.pendingResolves.get()

        // Wait for pending DNS resolves to finish and the minimum scan time to elapse...
        if (nowTime >= minDnsScanTime && !pendingResolves) {
          debug("DNS resolver done...")
          break
        }

        // Or if 10 seconds pass...
        if (nowTime >= maxTimeoutTime) {
          debug("DNS resolver took too long! Skipping...")
          break
        }

        debug("Awaiting DNS resolver...")

        Thread.sleep(1_000)
      }

      val adbPort = DnsDiscover.Companion.adbPort
      if (adbPort != null)
        debug("Best ADB port discovered: $adbPort")
      else
        debug("No ADB port discovered, fallback...")

      debug("Starting ADB server...")
      adb(false, listOf("start-server")).waitFor(1, TimeUnit.MINUTES)

      val waitProcess = if (adbPort != null)
        adb(false, listOf("connect", "localhost:$adbPort")).waitFor(1, TimeUnit.MINUTES)
      else
        adb(false, listOf("wait-for-device")).waitFor(1, TimeUnit.MINUTES)

      if (!waitProcess) {
        debug("Your device didn't connect to LADB")
        debug("If a reboot doesn't work, please contact support")

        if (isMobileDataAlwaysOnEnabled()) {
          debug("Please disable 'Mobile data always on' in Developer Settings!")
          Thread.sleep(5_000)
        }

        tryingToPair = false
        return false
      }
    }

    val deviceList = getDevices()
    Log.d("DEVICES", "Devices: $deviceList")

    shellProcess = if (autoShell) {
      var argList = listOf("shell")

      /* Uh oh, multiple possible devices... */
      if (deviceList.size > 1) {
        Log.w("DEVICES", "Multiple devices detected...")
        val localDevices = deviceList.filter { it ->
          it.contains("localhost")
        }

        /* Choose the first local device (hopefully the only). */
        if (localDevices.isNotEmpty()) {
          val serialId = localDevices.first()
          Log.w("DEVICES", "Choosing first local device: $serialId")
          argList = listOf("-s", serialId, "shell")
        } else {
          /*
           * If no local devices to use, try to filter out
           * any emulator devices and choose the first remaining result.
           */

          val nonEmulators = deviceList.filterNot { it ->
            it.contains("emulator")
          }

          /* Choose the first non emulator device (hopefully the only). */
          if (nonEmulators.isNotEmpty()) {
            val serialId = nonEmulators.first()
            Log.w("DEVICES", "Choosing first non-emulator device: $serialId")
            argList = listOf("-s", serialId, "shell")
          } else {
            /* Otherwise, we're screwed, just choose the first device. */
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

    if (autoShell)
      sendToShellProcess("echo 'Entered adb shell'")
    else
      sendToShellProcess("echo 'Entered non-adb shell'")

    val startupCommand = "echo 'Success! ※\\(^o^)/※'"
    if (startupCommand.isNotEmpty())
      sendToShellProcess(startupCommand)

    _running.postValue(true)
    tryingToPair = false


    // TODO: if app usage is not enabled or the user wants to use sysdump for better control
//    startAdbSysdumpMonitoring()

    if(!hasUsageStatsPermission(context)) {
      sendToShellProcess("appops set ${context.packageName} GET_USAGE_STATS allow")
    }

    appUsageForegroundWatcher.start()

    return true
  }

  private fun isWirelessDebuggingEnabled() =
    Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1

  private fun isUSBDebuggingEnabled() =
    Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

  private fun isMobileDataAlwaysOnEnabled() =
    Settings.Global.getInt(context.contentResolver, "mobile_data_always_on", 0) == 1

  /**
   * Settings.Global.MOBILE_DATA_ALWAYS_ON creates a bug
   * with the DNS resolver.
   */
  fun disableMobileDataAlwaysOn() {
    val secureSettingsGranted =
      context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    if (secureSettingsGranted) {
      // Only turn it off if it's already on.
      if (isMobileDataAlwaysOnEnabled()) {
        debug("Disabling 'Mobile data always on'...")
        Settings.Global.putInt(
          context.contentResolver,
          "mobile_data_always_on",
          0
        )
        Thread.sleep(3_000)
      }
    }
  }

  /**
   * Cycles wireless debugging to get a new port to scan.
   *
   * For whatever reason, Wireless Debugging needs to be
   * cycled twice to broadcast a valid port.
   */
  fun cycleWirelessDebugging() {
    val secureSettingsGranted =
      context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    if (secureSettingsGranted) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        debug("Cycling wireless debugging, please wait...")
        // Only turn it off if it's already on.
        if (isWirelessDebuggingEnabled()) {
          debug("Turning off wireless debugging...")
          Settings.Global.putInt(
            context.contentResolver,
            "adb_wifi_enabled",
            0
          )
          Thread.sleep(3_000)
        }

        debug("Turning on wireless debugging...")
        Settings.Global.putInt(
          context.contentResolver,
          "adb_wifi_enabled",
          1
        )
        Thread.sleep(3_000)

        debug("Turning off wireless debugging...")
        Settings.Global.putInt(
          context.contentResolver,
          "adb_wifi_enabled",
          0
        )
        Thread.sleep(3_000)

        debug("Turning on wireless debugging...")
        Settings.Global.putInt(
          context.contentResolver,
          "adb_wifi_enabled",
          1
        )
        Thread.sleep(3_000)
      }
    }
  }

  /**
   * Wait restart the shell once it dies
   */
  fun waitForDeathAndReset() {
    while (true) {
      /* Do not falsely claim the shell is dead if we haven't even initialized it yet */
      if (tryingToPair) continue

      shellProcess?.waitFor()
      _running.postValue(false)
      debug("Shell is dead, resetting...")
      adb(false, listOf("kill-server")).waitFor()

      Thread.sleep(3_000)
      initServer()
    }
  }

  /**
   * Ask the device to pair on Android 11+ devices
   */
  fun pair(port: String, pairingCode: String): Boolean {
    val pairShell = adb(false, listOf("pair", "localhost:$port"))

    /* Sleep to allow shell to catch up */
    Thread.sleep(5000)

    /* Pipe pairing code */
    PrintStream(pairShell.outputStream).apply {
      println(pairingCode)
      flush()
    }

    /* Continue once finished pairing (or 10s elapses) */
    pairShell.waitFor(10, TimeUnit.SECONDS)
    pairShell.destroyForcibly().waitFor()

    val killShell = adb(false, listOf("kill-server"))
    killShell.waitFor(3, TimeUnit.SECONDS)
    killShell.destroyForcibly()

    return pairShell.exitValue() == 0
  }

  /**
   * Send a raw ADB command
   */
  private fun adb(redirect: Boolean, command: List<String>): Process {
    val commandList = command.toMutableList().also {
      it.add(0, adbPath)
    }
    return shell(redirect, commandList)
  }

  /**
   * Send a raw shell command
   */
  private fun shell(redirect: Boolean, command: List<String>): Process {
    val processBuilder = ProcessBuilder(command)
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

  /**
   * Send commands directly to the shell process
   */
  fun sendToShellProcess(msg: String) {
    if (shellProcess == null || shellProcess?.outputStream == null)
      return
    PrintStream(shellProcess!!.outputStream!!).apply {
      println(msg)
      flush()
    }
  }

  /**
   * Write a debug message to the user
   */
  fun debug(msg: String) {
    synchronized(outputBufferFile) {
      Log.d("DEBUG", msg)
      if (outputBufferFile.exists())
        outputBufferFile.appendText("* $msg" + System.lineSeparator())
    }
  }

  fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
      AppOpsManager.OPSTR_GET_USAGE_STATS,
      android.os.Process.myUid(),
      context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
  }

  companion object {

    const val MAX_OUTPUT_BUFFER_SIZE = 1024 * 16
    const val OUTPUT_BUFFER_DELAY_MS = 100L

    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var instance: ADB? = null
    fun getInstance(context: Context): ADB = instance ?: synchronized(this) {
      instance ?: ADB(context).also { instance = it }
    }
  }
}