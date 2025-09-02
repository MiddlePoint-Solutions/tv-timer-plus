package io.middlepoint.tvsleep.services

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.ComposeLifecycleOwner
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.timer.TimeKeeper
import io.middlepoint.tvsleep.ui.components.MainTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class SleepService : Service() {
    private val logger = Logger.withTag("SleepService")
    private lateinit var timeKeeper: TimeKeeper
    private lateinit var wm: WindowManager
    private lateinit var overlayView: View
    private var composeView: ComposeView? = null
    private var composeOwner: ComposeLifecycleOwner? = null
    private lateinit var params: WindowManager.LayoutParams

    // 1. Force true for testing
    private val overlayVisibleStateFlow = MutableStateFlow(true)

    companion object {
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

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView =
            LayoutInflater
                .from(this)
                .inflate(R.layout.view_overlay, FrameLayout(this), false)
        // 3. Set a background color for testing visibility of overlayView itself
        overlayView.setBackgroundColor(android.graphics.Color.RED)


        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params =
            WindowManager
                .LayoutParams(
                    300, // 2. Fixed width for testing
                    300, // 2. Fixed height for testing
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 200 // initial position
                    y = 200
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

                    LaunchedEffect(overlayVisible) {
                        logger.d("Compose: overlayVisible state is: $overlayVisible")
                    }

                    if (overlayVisible) {
                        MaterialTheme {
                            val time by timeKeeper.currentTimerTotalDuration.collectAsState()
                            val tick by timeKeeper.tick.collectAsState()
                            val timerLabel by timeKeeper.timerLabel.collectAsState()
                            val timerScreenState by timeKeeper.timerState.collectAsState()

                            val progress = if (time > 0) (tick.toFloat() / time.toFloat()).coerceAtLeast(0f) else 0f
                            val progressOffset = (1 - progress)
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressOffset,
                                animationSpec =
                                    SpringSpec(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessVeryLow,
                                        visibilityThreshold = 1 / 1000f,
                                    ),
                                label = "TimerProgressAnimation",
                            )

                            MainTimer(
                                animatedProgress = animatedProgress,
                                formattedTime = timerLabel,
                                timerScreenState = timerScreenState,
                                modifier =
                                    Modifier
                                        .size(200.dp) // MainTimer has a defined size
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, drag ->
                                                change.consume()
                                                params.x += drag.x.roundToInt()
                                                params.y += drag.y.roundToInt()
                                                runCatching { wm.updateViewLayout(overlayView, params) }
                                                    .onFailure { logger.e(it) { "Error updating view layout" } }
                                            }
                                        },
                                onOptionTimerClick = { /* Decide action */ },
                            )
                        }
                    } else {
                        Box {} // Empty content when not visible
                        logger.d("Compose: Rendering empty Box as overlayVisible is false")
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

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        logger.d("onStartCommand action: ${intent?.action}")
        // Note: For testing, overlayVisibleStateFlow is hardcoded to true initially.
        // The SHOW/HIDE actions will change the state, but it starts as true.
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                logger.d("ACTION_SHOW_OVERLAY received")
                overlayVisibleStateFlow.value = true
            }
            ACTION_HIDE_OVERLAY -> {
                logger.d("ACTION_HIDE_OVERLAY received")
                overlayVisibleStateFlow.value = false
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        logger.d("onDestroy started")
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
}
