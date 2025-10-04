package io.middlepoint.tvsleep.events

import io.middlepoint.tvsleep.ui.screens.TimeOptionItem

sealed class MainActivityViewEvent {
    data class OnTimeSelected(val timeOptionItem: TimeOptionItem) : MainActivityViewEvent()
}
