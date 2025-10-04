package io.middlepoint.tvsleep.ui.screens

import androidx.lifecycle.ViewModel
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.timer.TimerController

sealed class TimeSelectionEvent {
    data class OnTimeSelected(val timeOptionItem: TimeOptionItem) : TimeSelectionEvent()
}

class TimeSelectionViewModel : ViewModel() {
    private val timeKeeper: TimerController = TimeKeeper.getInstance()

    fun onEvent(event: TimeSelectionEvent) {
        when (event) {
            is TimeSelectionEvent.OnTimeSelected -> {
                timeKeeper.selectTime(event.timeOptionItem)
            }
        }
    }
}
