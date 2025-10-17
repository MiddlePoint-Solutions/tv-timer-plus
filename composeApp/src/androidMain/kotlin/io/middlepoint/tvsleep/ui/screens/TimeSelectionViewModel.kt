package io.middlepoint.tvsleep.ui.screens

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.edit
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
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

sealed class TimeSelectionEvent {
    data class OnTimeSelected(
        val timeOptionItem: TimeOptionItem,
    ) : TimeSelectionEvent()

    data class OnAppSelected(
        val appInfo: AppInfo,
    ) : TimeSelectionEvent()

    object OnBackFromAppSelection : TimeSelectionEvent()

    object ShowCustomTimeDialog : TimeSelectionEvent()

    object HideCustomTimeDialog : TimeSelectionEvent()

    data class SaveCustomTime(
        val timeInMinutes: String,
        val label: String,
    ) : TimeSelectionEvent()

    data class OnTimeItemLongPress(
        val timeOptionItem: TimeOptionItem,
    ) : TimeSelectionEvent()

    data class OnDeleteItem(
        val timeOptionItem: TimeOptionItem,
    ) : TimeSelectionEvent()

    object OnCancelDelete : TimeSelectionEvent()

    object ShowEasterEgg : TimeSelectionEvent()
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

enum class SelectionMode {
    Time,
    App,
}

data class TimeSelectionState(
    val timeOptions: List<TimeOptionItem> = emptyList(),
    val showDialog: Boolean = false,
    val itemInDeleteMode: TimeOptionItem? = null,
    val showEasterEgg: Boolean = false,
    val selectionMode: SelectionMode = SelectionMode.Time,
    val installedApps: List<AppInfo> = emptyList(),
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

    fun onEvent(event: TimeSelectionEvent) {
        when (event) {
            is TimeSelectionEvent.OnTimeSelected -> onTimeSelected(event)
            is TimeSelectionEvent.OnAppSelected -> onAppSelected(event)
            is TimeSelectionEvent.OnBackFromAppSelection -> onBackFromAppSelection()
            is TimeSelectionEvent.ShowCustomTimeDialog -> showCustomTimeDialog()
            is TimeSelectionEvent.HideCustomTimeDialog -> hideCustomTimeDialog()
            is TimeSelectionEvent.SaveCustomTime -> saveCustomTime(event)
            is TimeSelectionEvent.OnTimeItemLongPress -> onTimeItemLongPress(event)
            is TimeSelectionEvent.OnDeleteItem -> onDeleteItem(event)
            is TimeSelectionEvent.OnCancelDelete -> onCancelDelete()
            is TimeSelectionEvent.ShowEasterEgg -> showEasterEgg()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm: PackageManager = getApplication<Application>().packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(mainIntent, 0)
        return packages.map {
            AppInfo(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm)
            )
        }
    }

    private fun loadTimeOptions() {
        viewModelScope.launch {
            val savedTimeOptions = sharedPreferences.getString("time_options", null)
            val initialTimeOptions =
                if (savedTimeOptions != null) {
                    Json.decodeFromString<List<TimeOptionItem>>(savedTimeOptions)
                } else {
                    val defaultTimeOptions = defaultTimeOptions
                    val jsonString = Json.encodeToString(defaultTimeOptions)
                    sharedPreferences.edit { putString("time_options", jsonString) }
                    defaultTimeOptions
                }
            _uiState.value = TimeSelectionState(timeOptions = initialTimeOptions)
        }
    }

    private fun onTimeSelected(event: TimeSelectionEvent.OnTimeSelected) {
        if (_uiState.value.itemInDeleteMode != null) {
            onCancelDelete()
        } else {
            timeKeeper.selectTime(event.timeOptionItem)
            _uiState.value = _uiState.value.copy(selectionMode = SelectionMode.App, installedApps = getInstalledApps())
        }
    }

    private fun onAppSelected(event: TimeSelectionEvent.OnAppSelected) {
        timeKeeper.selectApp(event.appInfo.packageName)
        viewModelScope.launch {
            delay(500L)
            val launchIntent = getApplication<Application>().packageManager.getLaunchIntentForPackage(event.appInfo.packageName)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(it)
            }
        }
    }

    private fun onBackFromAppSelection() {
        _uiState.value = _uiState.value.copy(selectionMode = SelectionMode.Time)
    }

    private fun showCustomTimeDialog() {
        _uiState.value = _uiState.value.copy(showDialog = true, itemInDeleteMode = null)
    }

    private fun hideCustomTimeDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    private fun saveCustomTime(event: TimeSelectionEvent.SaveCustomTime) {
        val timeInMinutes = event.timeInMinutes.toLongOrNull() ?: return

        val newTimeOption =
            TimeOptionItem(
                time = "$timeInMinutes min",
                label = event.label,
                timeInMillis = timeInMinutes.minutes.inWholeMilliseconds,
            )

        val updatedTimeOptions = listOf(newTimeOption) + _uiState.value.timeOptions

        val jsonString = Json.encodeToString(updatedTimeOptions)
        sharedPreferences.edit { putString("time_options", jsonString) }

        _uiState.value = _uiState.value.copy(timeOptions = updatedTimeOptions, showDialog = false)
    }

    private fun onTimeItemLongPress(event: TimeSelectionEvent.OnTimeItemLongPress) {
        _uiState.value = _uiState.value.copy(itemInDeleteMode = event.timeOptionItem)
    }

    private fun onDeleteItem(event: TimeSelectionEvent.OnDeleteItem) {
        val updatedTimeOptions = _uiState.value.timeOptions.filter { it != event.timeOptionItem }
        val jsonString = Json.encodeToString(updatedTimeOptions)
        sharedPreferences.edit { putString("time_options", jsonString) }
        _uiState.value = _uiState.value.copy(timeOptions = updatedTimeOptions, itemInDeleteMode = null)
    }

    private fun onCancelDelete() {
        _uiState.value = _uiState.value.copy(itemInDeleteMode = null)
    }

    private fun showEasterEgg() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showEasterEgg = true)
            delay(2000)
            _uiState.value = _uiState.value.copy(showEasterEgg = false)
        }
    }
}