package io.middlepoint.tvsleep.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.timer.TimerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

sealed class TimeSelectionEvent {
    data class OnTimeSelected(val timeOptionItem: TimeOptionItem) : TimeSelectionEvent()
    object ShowCustomTimeDialog : TimeSelectionEvent()
    object HideCustomTimeDialog : TimeSelectionEvent()
    data class SaveCustomTime(val timeInMinutes: String, val label: String) : TimeSelectionEvent()
    data class OnTimeItemLongPress(val timeOptionItem: TimeOptionItem) : TimeSelectionEvent()
    data class OnDeleteItem(val timeOptionItem: TimeOptionItem) : TimeSelectionEvent()
    object OnCancelDelete : TimeSelectionEvent()
    object ShowEasterEgg : TimeSelectionEvent()
}

data class TimeSelectionState(
    val timeOptions: List<TimeOptionItem> = emptyList(),
    val showDialog: Boolean = false,
    val itemInDeleteMode: TimeOptionItem? = null,
    val showEasterEgg: Boolean = false
)

class TimeSelectionViewModel(application: Application) : AndroidViewModel(application) {
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
            val initialTimeOptions = if (savedTimeOptions != null) {
                Json.decodeFromString<List<TimeOptionItem>>(savedTimeOptions)
            } else {
                val defaultTimeOptions = timeOptions
                val jsonString = Json.encodeToString(defaultTimeOptions)
                sharedPreferences.edit().putString("time_options", jsonString).apply()
                defaultTimeOptions
            }
            _uiState.value = TimeSelectionState(timeOptions = initialTimeOptions)
        }
    }

    fun onEvent(event: TimeSelectionEvent) {
        when (event) {
            is TimeSelectionEvent.OnTimeSelected -> {
                if (_uiState.value.itemInDeleteMode != null) {
                    onEvent(TimeSelectionEvent.OnCancelDelete)
                } else {
                    timeKeeper.selectTime(event.timeOptionItem)
                }
            }
            is TimeSelectionEvent.ShowCustomTimeDialog -> {
                _uiState.value = _uiState.value.copy(showDialog = true, itemInDeleteMode = null)
            }
            is TimeSelectionEvent.HideCustomTimeDialog -> {
                _uiState.value = _uiState.value.copy(showDialog = false)
            }
            is TimeSelectionEvent.SaveCustomTime -> {
                val timeInMinutes = event.timeInMinutes.toLongOrNull() ?: return

                val newTimeOption = TimeOptionItem(
                    time = "${timeInMinutes} min",
                    label = event.label,
                    timeInMillis = timeInMinutes.minutes.inWholeMilliseconds
                )

                val updatedTimeOptions = listOf(newTimeOption) + _uiState.value.timeOptions

                val jsonString = Json.encodeToString(updatedTimeOptions)
                sharedPreferences.edit().putString("time_options", jsonString).apply()

                _uiState.value = _uiState.value.copy(timeOptions = updatedTimeOptions, showDialog = false)
            }
            is TimeSelectionEvent.OnTimeItemLongPress -> {
                _uiState.value = _uiState.value.copy(itemInDeleteMode = event.timeOptionItem)
            }
            is TimeSelectionEvent.OnDeleteItem -> {
                val updatedTimeOptions = _uiState.value.timeOptions.filter { it != event.timeOptionItem }
                val jsonString = Json.encodeToString(updatedTimeOptions)
                sharedPreferences.edit().putString("time_options", jsonString).apply()
                _uiState.value = _uiState.value.copy(timeOptions = updatedTimeOptions, itemInDeleteMode = null)
            }
            is TimeSelectionEvent.OnCancelDelete -> {
                _uiState.value = _uiState.value.copy(itemInDeleteMode = null)
            }
            is TimeSelectionEvent.ShowEasterEgg -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(showEasterEgg = true)
                    delay(2000)
                    _uiState.value = _uiState.value.copy(showEasterEgg = false)
                }
            }
        }
    }
}
