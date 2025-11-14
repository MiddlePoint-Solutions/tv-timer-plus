package io.middlepoint.tvsleep.ui.screens.timer

import androidx.lifecycle.ViewModel
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.timer.TimeKeeper
import kotlinx.coroutines.flow.StateFlow

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
  
}
