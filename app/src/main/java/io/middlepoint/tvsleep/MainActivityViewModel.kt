package io.middlepoint.tvsleep

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.draco.ladb.utils.DnsDiscover
import io.middlepoint.tvsleep.utils.ADB
import io.middlepoint.tvsleep.utils.ONE_THOUSAND_INT
import io.middlepoint.tvsleep.utils.TimerState
import io.middlepoint.tvsleep.utils.ZERO_LONG
import io.middlepoint.tvsleep.utils.toHhMmSs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask

@Suppress("ktlint:standard:no-consecutive-comments")
class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val logger = Logger.withTag("MainActivityViewModel")

    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    private val _timerState = MutableLiveData<TimerState>()
    val timerState: LiveData<TimerState> = _timerState

    private val _tick = MutableLiveData<Long>()
    val tick: LiveData<Long> = _tick

    private val _timerLabel = MutableLiveData<String>()
    val timerLabel: LiveData<String> = _timerLabel

    private var timer: Timer? = null

    private val _homeState = MutableLiveData<HomeState>(HomeState.Idle)
    val homeState: LiveData<HomeState> = _homeState

    val isPairing = MutableLiveData<Boolean>()

    //    private var checker: PiracyChecker? = null
    private val sharedPreferences =
        PreferenceManager
            .getDefaultSharedPreferences(application.applicationContext)

    val adb = ADB.getInstance(getApplication<Application>().applicationContext)

    val dnsDiscover =
        DnsDiscover.getInstance(
            application.applicationContext,
            application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager,
        )

    private val _viewModelHasStartedADB = MutableLiveData(false)
    val viewModelHasStartedADB: LiveData<Boolean> = _viewModelHasStartedADB

    init {
        startOutputThread()
        dnsDiscover.scanAdbPorts()

        viewModelScope.launch {
            adb.state.observeForever { state ->
                _homeState.postValue(state.mapToHomeState())
            }
        }
    }

    fun startADBServer(callback: ((Boolean) -> (Unit))? = null) {
        // Don't start if it's already started.
        if (_viewModelHasStartedADB.value == true || adb.running.value == true) return

        viewModelScope.launch(Dispatchers.IO) {
            val success = adb.initServer()
            if (success) {
                startShellDeathThread()
                _viewModelHasStartedADB.postValue(true)
            }
            callback?.invoke(success)
        }
    }

    fun sendCommand(command: String) {
        adb.sendToShellProcess(command)
    }

    fun startTimer(duration: Long) {
        val delay = 0L
        val period = 250L
        timer = Timer()
        var interval = duration

        timer?.schedule(
            object : TimerTask() {
                override fun run() {
                    interval -= period
                    val newState = TimerState.Running(duration, interval)
//                    _timerState.postValue(newState)
                    _tick.postValue(interval)

                    if (interval % ONE_THOUSAND_INT == ZERO_LONG) _timerLabel.postValue(interval.toHhMmSs())
                    if (interval <= ZERO_LONG) {
                        if (_timerState.value != TimerState.Finished) _timerState.postValue(TimerState.Finished)
                    } else {
                        if (_timerState.value != TimerState.Started) _timerState.postValue(TimerState.Started)
                    }

                    if (interval == 0L) {
                        timer?.cancel()
                    } else if (interval < 0) {
//                        stopTimerService()
                    }
                }
            },
            delay,
            period,
        )
    }

    /**
     * Start the piracy checker if it is not setup yet (release builds only)
     *
     * @param activity Activity to use when showing the error
     */
//    fun piracyCheck(activity: Activity) {
//        if (checker != null || !BuildConfig.ANTI_PIRACY)
//            return
//
//        val context = getApplication<Application>().applicationContext
//
//        checker = activity.piracyChecker {
//            enableGooglePlayLicensing("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoRTOoEZ/IFfA/JkBFIrZqLq7N66JtJFTn/5C2QMO2EIY6hG4yZ5YTA3JrbJuuGVzQE8j29s6Lwu+19KKZcITTkZjfgl2Zku8dWQKZFt46f7mh8s1spzzc6rmSWIBPZUxN6fIIz8ar+wzyZdu3z+Iiy31dUa11Pyh82oOsWH7514AYGeIDDlvB1vSfNF/9ycEqTv5UAOgHxqZ205C1VVydJyCEwWWVJtQ+Z5zRaocI6NGaYRopyZteCEdKkBsZ69vohk4zr2SpllM5+PKb1yM7zfsiFZZanp4JWDJ3jRjEHC4s66elWG45yQi+KvWRDR25MPXhdQ9+DMfF2Ao1NTrgQIDAQAB")
//            saveResultToSharedPreferences(
//                sharedPreferences,
//                context.getString(R.string.pref_key_verified)
//            )
//        }
//
//        val verified = sharedPreferences.getBoolean(context.getString(R.string.pref_key_verified), false)
//        if (!verified)
//            checker?.start()
//    }

    /**
     * Continuously update shell output
     */
    private fun startOutputThread() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val out = readOutputFile(adb.outputBufferFile)
                val currentText = _outputText.value

                // TODO: add debug flag
                if (out != currentText) {
                    logger.d { "Output: $out" }
                    _outputText.postValue(out)
                }
                Thread.sleep(ADB.OUTPUT_BUFFER_DELAY_MS)
            }
        }
    }

    /**
     * Start a death listener to restart the shell once it dies
     */
    private fun startShellDeathThread() {
        viewModelScope.launch(Dispatchers.IO) {
            adb.waitForDeathAndReset()
        }
    }

    /**
     * Erase all shell text
     */
    fun clearOutputText() {
        adb.outputBufferFile.writeText("")
    }

    /**
     * Check if the user should be prompted to pair
     */
    fun needsToPair(): Boolean =
        !sharedPreferences.getBoolean(Constants.pairedKey, false) &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)

    fun setPairedBefore(value: Boolean) {
        val context = getApplication<Application>().applicationContext
        sharedPreferences.edit { putBoolean(Constants.pairedKey, value) }
    }

    /**
     * Read the content of the ABD output file
     */
    private fun readOutputFile(file: File): String {
        val out = ByteArray(adb.getOutputBufferSize())

        synchronized(file) {
            if (!file.exists()) {
                return ""
            }

            file.inputStream().use {
                val size = it.channel.size()

                if (size <= out.size) {
                    return String(it.readBytes())
                }

                val newPos = (it.channel.size() - out.size)
                it.channel.position(newPos)
                it.read(out)
            }
        }

        return String(out)
    }
}
