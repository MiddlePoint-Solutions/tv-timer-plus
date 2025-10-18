package io.middlepoint.tvsleep

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.services.SleepService
import io.middlepoint.tvsleep.timer.TimeKeeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TvSleepApplication :
    Application(),
    DefaultLifecycleObserver {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var timeKeeper: TimeKeeper
    private val log = Logger.withTag("TvSleepApplication")

    override fun onCreate() {
        super<Application>.onCreate()
        log.d { "onCreate" }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        timeKeeper = TimeKeeper.getInstance()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        log.d { "onStart" }
        // App came to foreground
        if (timeKeeper.timerState.value is TimerState.Started) {
            sendOverlayCommand(SleepService.ACTION_HIDE_OVERLAY)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        log.d { "onStop" }
        // App went to background
        if (timeKeeper.timerState.value is TimerState.Started) {
            sendOverlayCommand(SleepService.ACTION_SHOW_OVERLAY)
        }
    }

    private fun startSleepService() {
        Intent(this, SleepService::class.java).also { intent ->
            startService(intent)
        }
    }

    private fun stopSleepService() {
        Intent(this, SleepService::class.java).also { intent ->
            stopService(intent)
        }
        // Also ensure overlay is explicitly hidden when service stops
        sendOverlayCommand(SleepService.ACTION_HIDE_OVERLAY)
    }

    private fun sendOverlayCommand(action: String) {
        log.d { "sendOverlayCommand - action: $action" } // Add this log
        Intent(this, SleepService::class.java).also { intent ->
            intent.action = action
            startService(intent) // Using startService to send command
        }
    }
}
