package io.middlepoint.tvsleep

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.draco.ladb.utils.DnsDiscover
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.timer.TimerController
import io.middlepoint.tvsleep.ui.screens.TimeOptionItem
import io.middlepoint.tvsleep.utils.ADB
import io.middlepoint.tvsleep.utils.ADBOld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@Suppress("ktlint:standard:no-consecutive-comments")
class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val logger = Logger.withTag("MainActivityViewModel")

    private val _outputText = MutableStateFlow("")
    val outputText: StateFlow<String> = _outputText.asStateFlow()

    private val timeKeeper: TimerController = TimeKeeper.getInstance()

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Connecting)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    val isPairing = MutableStateFlow(false)

    private val sharedPreferences =
        PreferenceManager
            .getDefaultSharedPreferences(application.applicationContext)

    val adb = ADB.getInstance(getApplication<Application>().applicationContext)

    val dnsDiscover =
        DnsDiscover.getInstance(
            application.applicationContext,
            application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager,
        )

    private val _viewModelHasStartedADB = MutableStateFlow(false)
    val viewModelHasStartedADB: StateFlow<Boolean> = _viewModelHasStartedADB.asStateFlow()

    init {
        startOutputThread()
        dnsDiscover.scanAdbPorts()
        setupHomeStateFlow()
    }

    private fun setupHomeStateFlow() {
        setCurrentHomeState()
        viewModelScope.launch {
            combine(adb.state, timeKeeper.timerState) { adbState, timerControllerState ->
                deriveHomeState(timerControllerState, adbState)
            }.collect { combinedHomeState ->
                _homeState.value = combinedHomeState
            }
        }
    }

    private fun setCurrentHomeState() {
        _homeState.value = deriveHomeState(timeKeeper.timerState.value, adb.state.value)
    }

    private fun deriveHomeState(
        timerControllerState: TimerState,
        adbState: AdbState,
    ): HomeState {
        val isActive =
            timerControllerState !is TimerState.Stopped && timerControllerState !is TimerState.Finished
        return adbState.mapToHomeState(isTimerActive = isActive)
    }

    fun onTimeSelected(timeOptionItem: TimeOptionItem) {
        timeKeeper.selectTime(timeOptionItem) // Updated to pass the whole TimeOptionItem
    }

    fun handleMainScreenOptionButtonPressed() {
        // This action is initiated from the androidMain screen context.
        // If it's purely a timer modification, it could also be in TimerScreenViewModel,
        // but if it's a general quick action from home, it can stay.
        timeKeeper.addTime(60000L) // Add 1 minute
        // Potentially, this could also trigger a navigation to TimerScreen if not already there
        // or update HomeState to reflect that a timer has just been activated/modified.
        // For now, it just adds time.
    }

    fun startADBServer(callback: ((Boolean) -> (Unit))? = null) {
        if (_viewModelHasStartedADB.value || adb.running.value == true) return

        viewModelScope.launch(Dispatchers.IO) {
            val success = adb.initServer()
            if (success) {
                startShellDeathThread()
                _viewModelHasStartedADB.value = true
            }
            callback?.invoke(success)
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            adb.sendToShellProcess(command)
        }
    }

    private fun startOutputThread() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val out = readOutputFile(adb.outputBufferFile)
                val currentText = _outputText.value

                if (out != currentText) {
                    logger.d { "Output: $out" }
                    _outputText.value = out
                }
                delay(ADBOld.OUTPUT_BUFFER_DELAY_MS)
            }
        }
    }

    private fun startShellDeathThread() {
        viewModelScope.launch(Dispatchers.IO) {
            adb.waitForDeathAndReset()
        }
    }

    fun clearOutputText() {
        adb.outputBufferFile.writeText("")
        _outputText.value = ""
    }

    fun needsToPair(): Boolean =
        !sharedPreferences.getBoolean(Constants.pairedKey, false) &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)

    fun setPairedBefore(value: Boolean) {
        sharedPreferences.edit { putBoolean(Constants.pairedKey, value) }
    }

    private fun readOutputFile(file: File): String {
        val bufferSize = adb.getOutputBufferSize()
        val out = ByteArray(bufferSize)

        synchronized(file) {
            if (!file.exists()) {
                return ""
            }

            file.inputStream().use {
                val size = it.channel.size()

                return if (size <= out.size) {
                    String(it.readBytes())
                } else {
                    val newPos = (it.channel.size() - out.size)
                    it.channel.position(newPos)
                    val bytesRead = it.read(out)
                    if (bytesRead > 0) String(out, 0, bytesRead) else ""
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
