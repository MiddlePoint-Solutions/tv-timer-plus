package io.middlepoint.tvsleep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import io.middlepoint.tvsleep.TimerState

@Composable
fun MainTimer(
    animatedProgress: Float,
    timerScreenState: TimerState,
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {

        val localDensity = LocalDensity.current
        val fontSize = remember(formattedTime.length, constraints.maxWidth) {
            with(localDensity) {
                // Basic heuristic to fit the text in the circle.
                // The divisor can be tweaked for better visual results.
                (constraints.maxWidth / (formattedTime.length * 0.5f + 1f)).toSp()
            }
        }

        AnimatedVisibility(
            visible = timerScreenState != TimerState.Finished,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressWithThumb(
                progress = animatedProgress,
                strokeWidth = 4.dp,
                thumbSize = 6.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }

        ConstraintLayout(
            modifier = Modifier.fillMaxSize(),
        ) {
            val (timerText, timerImage) = createRefs()

            Text(
                text = formattedTime,
                style =
                MaterialTheme.typography.displaySmall.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Companion.W500,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.constrainAs(timerText) {
                    linkTo(
                        start = parent.start,
                        top = parent.top,
                        end = parent.end,
                        bottom = parent.bottom,
                    )
                },
            )
        }
    }
}
