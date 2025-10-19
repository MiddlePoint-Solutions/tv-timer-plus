package io.middlepoint.tvsleep.ui.screens.home

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
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

class HomeViewModel(
  application: Application,
) : AndroidViewModel(application) {
  private val timeKeeper: TimerController = TimeKeeper.getInstance()

  private val _uiState = MutableStateFlow(TimeSelectionState())
  val uiState: StateFlow<TimeSelectionState> = _uiState.asStateFlow()

  private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

  private val popularApps = listOf(
    "com.netflix.mediaclient",
    "com.netflix.ninja",
    "com.amazon.firetv.youtube",
    "com.google.android.youtube",
    "com.google.android.youtube.tv",
    "com.disney.disneyplus",
    "com.amazon.amazonvideo.livingroom",
    "com.hbo.max",
    "com.hbo.max.androidtv",
    "com.apple.atve.androidtv.appletv",
  )

  init {
    initOptions()
  }

  private fun initOptions() {
    loadTimeOptions()
    loadUserSelectedApps()
  }

  fun onEvent(event: TimeSelectionEvent) {
    when (event) {
      is TimeSelectionEvent.OnTimeSelected -> onTimeSelected(event)
      is TimeSelectionEvent.OnAppSelected -> onAppSelected(event)
      is TimeSelectionEvent.OnBackFromAppSelection -> onBackFromAppSelection()
      is TimeSelectionEvent.SaveCustomTime -> saveCustomTime(event)
      is TimeSelectionEvent.OnTimeItemLongPress -> onTimeItemLongPress(event)
      is TimeSelectionEvent.OnDeleteItem -> onDeleteItem(event)
      is TimeSelectionEvent.OnCancelDelete -> onCancelDelete()
      is TimeSelectionEvent.ShowEasterEgg -> showEasterEgg()
      is TimeSelectionEvent.StartTimerOnly -> startTimerOnly()
      is TimeSelectionEvent.OnAddAppsClicked -> onAddAppsClicked()
    }
  }

  private fun onAddAppsClicked() {
    _uiState.value = _uiState.value.copy(appSelectionMode = AppSelectionMode.All)
  }

  private fun startTimerOnly() {
    timeKeeper.start(null)
    reset()
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
      _uiState.value = _uiState.value.copy(timeOptions = initialTimeOptions)
    }
  }

  private fun loadUserSelectedApps() {
    viewModelScope.launch {
      val savedUserSelectedApps = sharedPreferences.getString("user_selected_apps", null)
      if (savedUserSelectedApps != null) {
        val appPackages = Json.decodeFromString<List<String>>(savedUserSelectedApps)
        val installedApps = getInstalledApps()
        val userSelectedApps = appPackages.mapNotNull { packageName ->
          installedApps.find { it.packageName == packageName }
        }
        _uiState.value = _uiState.value.copy(userSelectedApps = userSelectedApps)
      }
    }
  }

  private fun saveUserSelectedApps(appInfo: AppInfo) {
    val currentSelectedApps = _uiState.value.userSelectedApps
    if (currentSelectedApps.contains(appInfo)) return

    val updatedSelectedApps = currentSelectedApps + appInfo
    val appPackages = updatedSelectedApps.map { it.packageName }
    val jsonString = Json.encodeToString(appPackages)
    sharedPreferences.edit { putString("user_selected_apps", jsonString) }
    _uiState.value = _uiState.value.copy(
      userSelectedApps = updatedSelectedApps,
      appSelectionMode = AppSelectionMode.Curated
    )
  }

  private fun onTimeSelected(event: TimeSelectionEvent.OnTimeSelected) {
    if (_uiState.value.itemInDeleteMode != null) {
      onCancelDelete()
    } else {
      timeKeeper.selectTime(event.timeOptionItem)
      val installedApps = getInstalledApps()
      val popularApps = installedApps.filter { popularApps.contains(it.packageName) }

      _uiState.value = _uiState.value.copy(
        selectionMode = SelectionMode.App,
        installedApps = getInstalledApps(),
        popularApps = popularApps,
      )
    }
  }

  private fun onAppSelected(event: TimeSelectionEvent.OnAppSelected) {
    if (event.appInfo == AppInfo.ADD_APPS) {
      onAddAppsClicked()
      return
    }
    saveUserSelectedApps(event.appInfo)
    timeKeeper.start(event.appInfo.packageName)
    viewModelScope.launch {
      delay(500L)
      val launchIntent =
        getApplication<Application>().packageManager.getLaunchIntentForPackage(event.appInfo.packageName)
      launchIntent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(it)
      }
    }
    reset()
  }

  private fun onBackFromAppSelection() {
    if (_uiState.value.appSelectionMode == AppSelectionMode.All) {
      _uiState.value = _uiState.value.copy(appSelectionMode = AppSelectionMode.Curated)
    } else {
      _uiState.value = _uiState.value.copy(selectionMode = SelectionMode.Time)
    }
  }

  private fun saveCustomTime(event: TimeSelectionEvent.SaveCustomTime) {
    val timeInMinutes = event.timeInMinutes

    val newTimeOption =
      TimeOptionItem(
        time = "$timeInMinutes min",
        label = "Custom",
        timeInMillis = timeInMinutes.minutes.inWholeMilliseconds,
      )

    val updatedTimeOptions = listOf(newTimeOption) + _uiState.value.timeOptions

    val jsonString = Json.encodeToString(updatedTimeOptions)
    sharedPreferences.edit { putString("time_options", jsonString) }

    _uiState.value = _uiState.value.copy(timeOptions = updatedTimeOptions)
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

  private fun reset() {
    viewModelScope.launch {
      delay(300)
      _uiState.value = TimeSelectionState()
      initOptions()
    }
  }

}
