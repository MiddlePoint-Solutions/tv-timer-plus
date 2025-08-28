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
import kotlin.time.Duration.Companion.milliseconds

@Suppress("ktlint:standard:no-consecutive-comments")
class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val logger = Logger.withTag("MainActivityViewModel")

    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    private val _timerState = MutableLiveData<TimerState>(TimerState.Stopped)
    val timerState: LiveData<TimerState> = _timerState

    private val _tick = MutableLiveData<Long>()
    val tick: LiveData<Long> = _tick

    private val _timerLabel = MutableLiveData<String>()
    val timerLabel: LiveData<String> = _timerLabel

    private var timer: Timer? = null

    private val _homeState = MutableLiveData<HomeState>()
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

    private val _currentTimerTotalDuration = MutableLiveData<Long>()
    val currentTimerTotalDuration: LiveData<Long> = _currentTimerTotalDuration

    init {
        startOutputThread()
        dnsDiscover.scanAdbPorts()

        viewModelScope.launch {
            adb.state.observeForever { state ->
                _homeState.postValue(state.mapToHomeState())
            }
        }
    }

    fun onTimeSelected(durationMillis: Long) {
        _currentTimerTotalDuration.postValue(durationMillis)
        _timerLabel.postValue(durationMillis.toHhMmSs())
        _tick.postValue(durationMillis) // Initialize tick to full duration
        startTimer(durationMillis)
        _timerState.postValue(TimerState.Started)
    }

    fun togglePlayPause() {
        when (_timerState.value) {
            is TimerState.Started -> {
                timer?.cancel()
                _timerState.postValue(TimerState.Paused)
            }
            is TimerState.Paused -> {
                // Resume timer
                val currentTick = _tick.value ?: return
                if (currentTick > 0) {
                    startTimer(currentTick) // Restart timer with remaining duration
                    _timerState.postValue(TimerState.Started)
                }
            }
            else -> {
                // Do nothing or log error
                logger.w { "togglePlayPause called in an unexpected state: ${_timerState.value}" }
            }
        }
    }

    fun stopTimerAndGoToSetup() {
        timer?.cancel()
        _timerState.postValue(TimerState.Stopped)
        // Reset relevant LiveData for a fresh start in TimerSetup
        _tick.postValue(0L)
        _timerLabel.postValue("")
        _currentTimerTotalDuration.postValue(0L)
    }

    // Placeholder for actual implementation based on requirements
    fun handleMainScreenOptionButtonPressed() {
        // Example: Add 1 minute to the timer if it's running or paused
        // Or reset the timer if it's finished
        when (val currentState = _timerState.value) {
            is TimerState.Started -> {
                // Add 1 minute
                val newDuration = (_tick.value ?: 0L) + 60000L
                _currentTimerTotalDuration.postValue((_currentTimerTotalDuration.value ?: 0L) + 60000L)
                timer?.cancel()
                startTimer(newDuration)
            }
            is TimerState.Paused -> {
                // Add 1 minute to paused time
                val newTick = (_tick.value ?: 0L) + 60000L
                _tick.postValue(newTick)
                _timerLabel.postValue(newTick.toHhMmSs())
                _currentTimerTotalDuration.postValue((_currentTimerTotalDuration.value ?: 0L) + 60000L)
            }
            is TimerState.Finished -> {
                // Reset to setup screen
                stopTimerAndGoToSetup()
            }
            else -> {
                logger.w { "Option button pressed in unexpected state: $currentState" }
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

    private fun startTimer(duration: Long) {
        timer?.cancel() // Cancel any existing timer
        val delay = 0L
        val period = 250L // Consider making this a constant
        timer = Timer()
        var interval = duration
        _tick.postValue(interval) // Ensure tick is updated immediately

        timer?.schedule(
            object : TimerTask() {
                override fun run() {
                    interval -= period
                    _tick.postValue(interval)

                    if (interval % ONE_THOUSAND_INT == ZERO_LONG || interval == duration) { // Update label also at the start
                        _timerLabel.postValue(interval.toHhMmSs())
                    }

                    if (interval <= ZERO_LONG) {
                        timer?.cancel()
                        if (_timerState.value != TimerState.Finished) {
                            _timerState.postValue(TimerState.Finished)
                        }
                    } else {
                        // Ensure state is started if not already
                        if (_timerState.value !is TimerState.Started) {
                            _timerState.postValue(TimerState.Started)
                        }
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
