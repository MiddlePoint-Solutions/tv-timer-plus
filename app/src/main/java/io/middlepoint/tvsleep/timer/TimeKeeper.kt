package io.middlepoint.tvsleep.timer

import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.utils.ONE_THOUSAND_INT
import io.middlepoint.tvsleep.utils.TimerState
import io.middlepoint.tvsleep.utils.ZERO_LONG
import io.middlepoint.tvsleep.utils.toHhMmSs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
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
            get() = GlobalScope.coroutineContext

        private val logger = Logger.withTag("TimeKeeper")

        private val _timerState = MutableStateFlow<TimerState>(TimerState.Stopped)
        override val timerState: StateFlow<TimerState> = _timerState

        private val _tick = MutableStateFlow<Long>(0L)
        override val tick: StateFlow<Long> = _tick

        private val _timerLabel = MutableStateFlow<String>("")
        override val timerLabel: StateFlow<String> = _timerLabel

        private var timerJob: Job? = null

        private val _currentTimerTotalDuration = MutableStateFlow<Long>(0L)
        override val currentTimerTotalDuration: StateFlow<Long> = _currentTimerTotalDuration

        override fun selectTime(durationMillis: Long) {
            _currentTimerTotalDuration.value = durationMillis
            _timerLabel.value = durationMillis.toHhMmSs()
            _tick.value = durationMillis // Initialize tick to full duration
            startTimer(durationMillis)
            _timerState.value = TimerState.Started
        }

        override fun togglePlayPause() {
            when (_timerState.value) {
                is TimerState.Started -> {
                    timerJob?.cancel()
                    _timerState.value = TimerState.Paused
                }

                is TimerState.Paused -> {
                    val currentTick = _tick.value
                    if (currentTick > 0) {
                        startTimer(currentTick)
                        _timerState.value = TimerState.Started
                    }
                }

                else -> {
                    logger.w { "togglePlayPause called in an unexpected state: ${_timerState.value}" }
                }
            }
        }

        override fun stopTimerAndReset() {
            timerJob?.cancel()
            _timerState.value = TimerState.Stopped
            _tick.value = 0L
            _timerLabel.value = ""
            _currentTimerTotalDuration.value = 0L
        }

        override fun addTime(durationMillis: Long) {
            when (val currentState = _timerState.value) {
                is TimerState.Started -> {
                    val newDuration = (_tick.value) + durationMillis
                    _currentTimerTotalDuration.value = (_currentTimerTotalDuration.value) + durationMillis
                    timerJob?.cancel()
                    startTimer(newDuration)
                }

                is TimerState.Paused -> {
                    val newTick = (_tick.value) + durationMillis
                    _tick.value = newTick
                    _timerLabel.value = newTick.toHhMmSs()
                    _currentTimerTotalDuration.value = (_currentTimerTotalDuration.value) + durationMillis
                }

                is TimerState.Finished -> {
                    stopTimerAndReset()
                }

                else -> {
                    logger.w { "addTime called in unexpected state: $currentState" }
                }
            }
        }

        override fun handleOptionClick() {
            // Placeholder: For example, add 1 minute (60,000 ms)
            // You can define a more specific behavior here.
            logger.i { "handleOptionClick called. Placeholder: Adding 1 minute." }
            addTime(60000L)
        }

        private fun startTimer(duration: Long) {
            timerJob?.cancel()
            val period = 250L
            var interval = duration
            _tick.value = interval

            timerJob =
                launch {
                    while (isActive && interval > ZERO_LONG) {
                        delay(period)
                        interval -= period
                        _tick.value = interval

                        if (interval % ONE_THOUSAND_INT == ZERO_LONG || interval == duration) {
                            _timerLabel.value = interval.toHhMmSs()
                        }

                        if (_timerState.value !is TimerState.Started && interval > ZERO_LONG) {
                            _timerState.value = TimerState.Started // Ensure state is started if timer is running
                        }
                    }
                    if (interval <= ZERO_LONG) {
                        if (_timerState.value != TimerState.Finished) {
                            _timerState.value = TimerState.Finished
                        }
                    }
                }
        }

        fun onDestroy() {
            timerJob?.cancel()
            // externalScope should be cancelled by its owner (e.g., ViewModel's viewModelScope)
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
