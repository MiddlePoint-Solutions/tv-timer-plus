package io.middlepoint.tvsleep.timer

import io.middlepoint.tvsleep.utils.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerController {
    val timerState: StateFlow<TimerState>
    val tick: StateFlow<Long>
    val timerLabel: StateFlow<String>
    val currentTimerTotalDuration: StateFlow<Long>
    val timerProgressOffset: StateFlow<Float> // Added for progress

    fun selectTime(durationMillis: Long)
    fun togglePlayPause()
    fun stopTimerAndReset()
    fun addTime(durationMillis: Long)
    fun handleOptionClick() // Added for the "Fix" button or similar option
}
