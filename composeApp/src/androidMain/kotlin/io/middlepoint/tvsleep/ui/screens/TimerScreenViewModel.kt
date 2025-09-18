package io.middlepoint.tvsleep.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.timer.TimerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimerScreenViewModel : ViewModel() {
    private val timeKeeper: TimeKeeper = TimeKeeper.getInstance() // Changed to TimeKeeper to access selectedTimeOptionLabel

    val timerScreenState: StateFlow<TimerState> = timeKeeper.timerState
    val timerLabel: StateFlow<String> = timeKeeper.timerLabel // This is the HH:MM:SS label
    val selectedTimeOptionLabel: StateFlow<String> = timeKeeper.selectedTimeOptionLabel // This is the custom label
    val timerProgressOffset: StateFlow<Float> = timeKeeper.timerProgressOffset

    // Expose a combined state if TimerScreen needs multiple pieces of data cohesively
    // For now, exposing individual flows from TimeKeeper as done above is fine.

    fun onActionClick() {
        timeKeeper.togglePlayPause()
    }

    fun onDelete() {
        timeKeeper.stopTimerAndReset()
    }

    fun onOptionTimerClick() {
        // Assuming a new method in TimeKeeper for this, e.g., add a fixed amount of time
        // or a specific "fix" operation. For now, let's call a new placeholder method.
        timeKeeper.handleOptionClick()
    }
}
