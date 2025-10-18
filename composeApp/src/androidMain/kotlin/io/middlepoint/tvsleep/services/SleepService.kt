package io.middlepoint.tvsleep.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.ui.components.MainTimer
import io.middlepoint.tvsleep.utils.ADB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit

class SleepService : Service() {
  private val logger = Logger.withTag("SleepService")
  private lateinit var timeKeeper: TimeKeeper
  private lateinit var adb: ADB
  private lateinit var wm: WindowManager
  private lateinit var overlayView: View
  private var composeView: ComposeView? = null
  private var composeOwner: ComposeLifecycleOwner? = null
  private lateinit var params: WindowManager.LayoutParams

  private val overlayVisibleStateFlow = MutableStateFlow(false)

  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var autoHideJob: Job? = null

  private var isForegroundServiceStarted = false

  companion object {
    private const val NOTIFICATION_ID = 1
    private const val AUTO_HIDE_DELAY_MS = 10_000L

    const val ACTION_SHOW_OVERLAY = "io.middlepoint.tvsleep.services.SHOW_OVERLAY"
    const val ACTION_HIDE_OVERLAY = "io.middlepoint.tvsleep.services.HIDE_OVERLAY"
  }

  override fun onCreate() {
    super.onCreate()

    logger.d("onCreate")
    if (!Settings.canDrawOverlays(this)) {
      logger.w("Cannot draw overlays, stopping service.")
      stopSelf()
      return
    }

    logger.d("onCreate - Settings.canDrawOverlays(this) is true")

    timeKeeper = TimeKeeper.getInstance()
    adb = ADB.getInstance(this)

    observeTimerState()
    observeFinalMinute()

    wm = getSystemService(WINDOW_SERVICE) as WindowManager
    overlayView =
      LayoutInflater
        .from(this)
        .inflate(R.layout.view_overlay, FrameLayout(this), false)

    val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    params =
      WindowManager
        .LayoutParams(
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          type,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
          PixelFormat.TRANSLUCENT,
        ).apply {
          gravity = Gravity.TOP or Gravity.START
          x = 40
          y = 40
        }

    wm.addView(overlayView, params)
    logger.d("OverlayView added to WindowManager")

    composeOwner = ComposeLifecycleOwner()
    composeOwner?.attachToDecorView(overlayView)

    composeView =
      ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
          val overlayVisible by overlayVisibleStateFlow.collectAsState()

          AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(tween(700)),
            exit = fadeOut(tween(1000)),
          ) {
            MaterialTheme {
              val timerLabel by timeKeeper.timerLabel.collectAsState()
              val timerScreenState by timeKeeper.timerState.collectAsState() // Keep this for MainTimer
              val timerProgressOffset by timeKeeper.timerProgressOffset.collectAsState()

              val animatedProgress by animateFloatAsState(
                targetValue = timerProgressOffset,
                animationSpec =
                  SpringSpec(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                    visibilityThreshold = 1 / 1000f,
                  ),
                label = "TimerProgressAnimation",
              )

              Row {
                MainTimer(
                  animatedProgress = animatedProgress,
                  formattedTime = timerLabel,
                  timerScreenState = timerScreenState,
                  modifier =
                    Modifier
                      .size(160.dp) // MainTimer has a defined size
                      .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                      )
                      .clipToBounds()
                      .padding(8.dp),
                )

                Image(
                  painter = painterResource(id = R.mipmap.ic_timer_logo),
                  contentDescription = "Timer Icon",
                  modifier = Modifier
                    .size(58.dp)
                    .absoluteOffset(x = (-22).dp)
                )
              }
            }
          }
        }
      }

    (overlayView as ViewGroup).addView(composeView)
    logger.d("ComposeView added to overlayView")
    composeOwner?.onCreate()
    composeOwner?.onStart()
    composeOwner?.onResume()
    logger.d("SleepService onCreate finished")
  }

  private fun observeTimerState() {
    serviceScope.launch {
      timeKeeper.timerState.collectLatest { state ->
        if (state is TimerState.Finished) {
          logger.d("Timer finished. Putting device to sleep.")
          try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            prefs.edit { putBoolean("show_review", true) }
            logger.d("Set show_review preference to true.")
            adb.goToSleep()
          } catch (e: Exception) {
            logger.e(e) { "Error putting device to sleep" }
          }
        }
      }
    }
  }

  private fun observeFinalMinute() {
    serviceScope.launch {
      timeKeeper.isInFinalMinute.collectLatest { isInFinalMinute ->
        if (isInFinalMinute) {
          overlayVisibleStateFlow.value = true
        } else {
          // Optionally, hide the overlay when not in the final minute,
          // depending on the desired behavior.
          // overlayVisibleStateFlow.value = false
        }
      }
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    if (!isForegroundServiceStarted) {
      startForeground(NOTIFICATION_ID, createNotification())
      isForegroundServiceStarted = true
    }
    logger.d("onStartCommand action: ${intent?.action}")
    when (intent?.action) {
      ACTION_SHOW_OVERLAY -> {
        logger.d("ACTION_SHOW_OVERLAY received")
        autoHideJob?.cancel()
        overlayVisibleStateFlow.value = true
        startAutoHideJob()
      }

      ACTION_HIDE_OVERLAY -> {
        logger.d("ACTION_HIDE_OVERLAY received")
        autoHideJob?.cancel()
        overlayVisibleStateFlow.value = false
      }
    }
    return START_STICKY
  }

  private fun startAutoHideJob() {
    autoHideJob =
      serviceScope.launch {
        delay(AUTO_HIDE_DELAY_MS)
        overlayVisibleStateFlow.value = false
        logger.d("Overlay hidden after delay")
      }
  }

  override fun onDestroy() {
    logger.d("onDestroy started")
    autoHideJob?.cancel()
    serviceScope.cancel() // Cancel the scope and all its children
    composeOwner?.onPause()
    composeOwner?.onStop()
    composeOwner?.onDestroy()
    composeView?.let { cv ->
      (overlayView as? ViewGroup)?.removeView(cv)
      runCatching { wm.removeView(overlayView) }
        .onFailure { logger.e(it) { "Error removing overlayView from WindowManager" } }
    }
    composeView = null
    composeOwner = null
    super.onDestroy()
    logger.d("onDestroy finished")
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun createNotification(): Notification {
    val channelId = "WebServerServiceChannel"
    val channelName = "Web Server Service"
    val chan =
      NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT,
      )
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(chan)

    return NotificationCompat
      .Builder(this, channelId)
      .setContentTitle("TV Sleep Server")
      .setContentText("Web server is running.")
      .setSmallIcon(R.mipmap.ic_launcher)
      .build()
  }
}
