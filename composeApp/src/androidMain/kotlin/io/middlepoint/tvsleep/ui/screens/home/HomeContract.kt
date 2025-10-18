package io.middlepoint.tvsleep.ui.screens.home

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

sealed class TimeSelectionEvent {
    data class OnTimeSelected(
        val timeOptionItem: TimeOptionItem,
    ) : TimeSelectionEvent()

    data class OnAppSelected(
        val appInfo: AppInfo,
    ) : TimeSelectionEvent()

    object StartTimerOnly : TimeSelectionEvent()

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

    object OnAddAppsClicked : TimeSelectionEvent()
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
) {
    companion object {
        val ADD_APPS = AppInfo("add_apps", "Add Apps", null)
    }
}

enum class SelectionMode {
    Time,
    App,
}

enum class AppSelectionMode {
    Curated,
    All,
}

data class TimeSelectionState(
    val timeOptions: List<TimeOptionItem> = emptyList(),
    val showDialog: Boolean = false,
    val itemInDeleteMode: TimeOptionItem? = null,
    val showEasterEgg: Boolean = false,
    val selectionMode: SelectionMode = SelectionMode.Time,
    val installedApps: List<AppInfo> = emptyList(),
    val popularApps: List<AppInfo> = emptyList(),
    val userSelectedApps: List<AppInfo> = emptyList(),
    val appSelectionMode: AppSelectionMode = AppSelectionMode.Curated,
)
