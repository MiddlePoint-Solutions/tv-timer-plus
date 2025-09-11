package io.middlepoint.tvsleep.timer

import io.middlepoint.tvsleep.ui.screens.TimeOptionItem
import io.middlepoint.tvsleep.utils.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerController {
    val timerState: StateFlow<TimerState>
    val tick: StateFlow<Long>
    val timerLabel: StateFlow<String> // This will hold the HH:MM:SS string
    // Consider adding another StateFlow for the TimeOptionItem.label if needed directly in the interface
    val currentTimerTotalDuration: StateFlow<Long>
    val timerProgressOffset: StateFlow<Float> // Added for progress

    fun selectTime(timeOptionItem: TimeOptionItem) // Changed from durationMillis: Long
    fun togglePlayPause()
    fun stopTimerAndReset()
    fun addTime(durationMillis: Long)
    fun handleOptionClick() // Added for the "Fix" button or similar option
}
