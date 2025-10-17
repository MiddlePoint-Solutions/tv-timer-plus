package io.middlepoint.tvsleep.timer

import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.ONE_THOUSAND_INT
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.ZERO_LONG
import io.middlepoint.tvsleep.toHhMmSs
import io.middlepoint.tvsleep.ui.screens.TimeOptionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class TimeKeeper private constructor() :
    TimerController,
    CoroutineScope {

    @OptIn(DelicateCoroutinesApi::class)
    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext + SupervisorJob()

    private val logger = Logger.withTag("TimeKeeper")

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Stopped)
    override val timerState: StateFlow<TimerState> = _timerState

    private val _tick = MutableStateFlow(0L)
    override val tick: StateFlow<Long> = _tick

    private val _timerLabel = MutableStateFlow("") // This is for HH:MM:SS
    override val timerLabel: StateFlow<String> = _timerLabel

    private val _selectedTimeOptionLabel = MutableStateFlow("")
    val selectedTimeOptionLabel: StateFlow<String> = _selectedTimeOptionLabel

    private val _selectedAppPackageName = MutableStateFlow<String?>(null)
    val selectedAppPackageName: StateFlow<String?> = _selectedAppPackageName

    private var timerJob: Job? = null

    private val _currentTimerTotalDuration = MutableStateFlow(0L)
    override val currentTimerTotalDuration: StateFlow<Long> = _currentTimerTotalDuration

    private val _timerProgressOffset = MutableStateFlow(1f)
    override val timerProgressOffset: StateFlow<Float> = _timerProgressOffset

    private fun setTimerState(newState: TimerState) {
        _timerState.value = newState
    }

    private fun updateProgressOffset() {
        val total = _currentTimerTotalDuration.value
        val current = _tick.value
        _timerProgressOffset.value =
            if (total > 0L) (1f - (current.toFloat() / total.toFloat())).coerceIn(0f, 1f) else 1f
    }

    override fun selectTime(timeOptionItem: TimeOptionItem) {
        _currentTimerTotalDuration.value = timeOptionItem.timeInMillis
        _selectedTimeOptionLabel.value = timeOptionItem.label // Store the custom label
        _timerLabel.value = timeOptionItem.timeInMillis.toHhMmSs() // Keep HH:MM:SS label
        _tick.value = timeOptionItem.timeInMillis // Initialize tick to full duration (remaining time)
        updateProgressOffset()
    }

    override fun selectApp(packageName: String) {
        _selectedAppPackageName.value = packageName
        setTimerState(TimerState.Started)
        startTimer(_tick.value)
    }

    override fun togglePlayPause() {
        when (_timerState.value) {
            is TimerState.Started -> {
                timerJob?.cancel()
                setTimerState(TimerState.Paused)
            }

            is TimerState.Paused -> {
                val currentTick = _tick.value
                if (currentTick > 0) {
                    setTimerState(TimerState.Started)
                    startTimer(currentTick)
                }
            }

            else -> {
                logger.w { "togglePlayPause called in an unexpected state: ${_timerState.value}" }
            }
        }
    }

    override fun stopTimerAndReset() {
        timerJob?.cancel()
        setTimerState(TimerState.Stopped)
        _tick.value = 0L
        _timerLabel.value = ""
        _selectedTimeOptionLabel.value = ""
        _selectedAppPackageName.value = null
        _currentTimerTotalDuration.value = 0L
        updateProgressOffset()
    }

    override fun addTime(durationMillis: Long) {
        val originalTotal = _currentTimerTotalDuration.value
        val currentTick = _tick.value
        // Adding time does not change the original selectedTimeOptionLabel

        when (val currentState = _timerState.value) {
            is TimerState.Started -> {
                val newTick = currentTick + durationMillis
                _currentTimerTotalDuration.value = originalTotal + durationMillis
                _tick.value = newTick
                updateProgressOffset()
                timerJob?.cancel() // Cancel before starting a new one
                startTimer(newTick)
            }

            is TimerState.Paused -> {
                val newTick = currentTick + durationMillis
                _tick.value = newTick
                _timerLabel.value = newTick.toHhMmSs()
                _currentTimerTotalDuration.value = originalTotal + durationMillis
                updateProgressOffset()
            }

            is TimerState.Finished -> { // If finished and time is added, it's like starting a new timer but without a TimeOptionItem
                _currentTimerTotalDuration.value = durationMillis
                _tick.value = durationMillis
                _timerLabel.value = durationMillis.toHhMmSs()
                _selectedTimeOptionLabel.value = "Added time" // Or some generic label
                updateProgressOffset()
                setTimerState(TimerState.Started)
                startTimer(durationMillis)
            }

            else -> {
                logger.w { "addTime called in unexpected state: $currentState" }
            }
        }
    }

    override fun handleOptionClick() {
        logger.i { "handleOptionClick called. Adding 60 seconds." }
        addTime(60000L)
    }

    private fun startTimer(duration: Long) { // duration is remaining time
        timerJob?.cancel()
        val period = 250L
        var interval = duration
        _tick.value = interval
        updateProgressOffset()

        timerJob =
            launch {
                while (isActive && interval > ZERO_LONG) {
                    delay(period)
                    interval -= period
                    _tick.value = interval
                    updateProgressOffset()

                    if (interval % ONE_THOUSAND_INT == ZERO_LONG || interval == duration) {
                        _timerLabel.value = interval.toHhMmSs()
                    }
                }
                if (interval <= ZERO_LONG) {
                    _tick.value = 0L
                    updateProgressOffset()
                    if (_timerState.value is TimerState.Started || _timerState.value is TimerState.Paused) {
                        delay(1000)
                        setTimerState(TimerState.Finished)
                    }
                }
            }
    }

    fun onDestroy() {
        timerJob?.cancel()
    }

    companion object {
        @Suppress("ktlint:standard:property-naming")
        @Volatile
        private var INSTANCE: TimeKeeper? = null

        fun getInstance(): TimeKeeper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeKeeper().also { INSTANCE = it }
            }
    }
}