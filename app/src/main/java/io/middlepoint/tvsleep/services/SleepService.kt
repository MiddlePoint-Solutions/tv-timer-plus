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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.ComposeLifecycleOwner
import io.middlepoint.tvsleep.R
import kotlin.math.roundToInt

class SleepService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var overlayView: View
    private var composeView: ComposeView? = null
    private var composeOwner: ComposeLifecycleOwner? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()

        // Permission guard
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

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
                    x = 200 // initial position
                    y = 200
                }

//    wm.addView(overlayView, params)
        wm.addView(overlayView, params)

        composeOwner = ComposeLifecycleOwner()
        composeOwner?.attachToDecorView(overlayView)

        val cv =
            ComposeView(this).apply {
                // Ensure the composition is disposed when the view is removed
//      setViewTreeLifecycleOwner(lifecycleOwner)
//      setViewTreeViewModelStoreOwner(lifecycleOwner)
//      setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnDetachedFromWindow,
                )

                setContent {
                    MaterialTheme {
                        OverlayBubble(
                            onDrag = { dx, dy ->
                                params.x += dx.roundToInt()
                                params.y += dy.roundToInt()
                                runCatching { wm.updateViewLayout(this, params) }
                            },
                            onClose = { stopSelf() },
                        )
                    }
                }
            }

        composeView = cv

        (overlayView as ViewGroup).addView(composeView)

        composeOwner?.let {
            it.onCreate()
            it.onStart()
            it.onResume()
        }
    }

    override fun onDestroy() {
        composeView?.let { v ->
            runCatching { wm.removeView(v) }
        }
        composeView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/** Simple draggable Compose bubble **/
@Composable
private fun OverlayBubble(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 4.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .background(Color(0xFF2A2A2A), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume() // prevent down-stream handling
                            onDrag(drag.x, drag.y)
                        }
                    }.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = "◉ Overlay\n(Drag me)\nTap to close",
                color = Color.White,
            )
        }
    }
    // If you want a tap-to-close, wrap with clickable:
    // .clickable { onClose() }
}
