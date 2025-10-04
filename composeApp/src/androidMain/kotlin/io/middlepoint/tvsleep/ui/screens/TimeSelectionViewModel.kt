package io.middlepoint.tvsleep.ui.screens

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.timer.TimerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed class TimeSelectionEvent {
    data class OnTimeSelected(
        val timeOptionItem: TimeOptionItem,
    ) : TimeSelectionEvent()
}

data class TimeSelectionState(
    val timeOptions: List<TimeOptionItem> = emptyList(),
)

class TimeSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val timeKeeper: TimerController = TimeKeeper.getInstance()

    private val _uiState = MutableStateFlow(TimeSelectionState())
    val uiState: StateFlow<TimeSelectionState> = _uiState.asStateFlow()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        loadTimeOptions()
    }

    private fun loadTimeOptions() {
        viewModelScope.launch {
            val savedTimeOptions = sharedPreferences.getString("time_options", null)
            if (savedTimeOptions != null) {
                val timeOptions = Json.decodeFromString<List<TimeOptionItem>>(savedTimeOptions)
                _uiState.value = TimeSelectionState(timeOptions = timeOptions)
            } else {
                // Save default time options
                val defaultTimeOptions = defaultTimeOptions
                val jsonString = Json.encodeToString(defaultTimeOptions)
                sharedPreferences.edit { putString("time_options", jsonString) }
                _uiState.value = TimeSelectionState(timeOptions = defaultTimeOptions)
            }
        }
    }

    fun onEvent(event: TimeSelectionEvent) {
        when (event) {
            is TimeSelectionEvent.OnTimeSelected -> {
                timeKeeper.selectTime(event.timeOptionItem)
            }
        }
    }
}
