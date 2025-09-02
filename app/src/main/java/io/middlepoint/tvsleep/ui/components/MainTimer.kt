package io.middlepoint.tvsleep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.utils.TimerState
import io.middlepoint.tvsleep.utils.calculateFontSize

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainTimer(
    animatedProgress: Float,
    timerScreenState: TimerState,
    formattedTime: String,
    onOptionTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Companion.Center) {
        AnimatedVisibility(
            visible = timerScreenState != TimerState.Finished,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressWithThumb(
                progress = animatedProgress,
                strokeWidth = 4.dp,
                thumbSize = 6.dp,
                modifier = Modifier.Companion.fillMaxSize(),
            )
        }

        ConstraintLayout(
            modifier = Modifier.Companion.fillMaxSize(),
        ) {
            val (timerText, optionTimerButton) = createRefs()

            val color =
                if (timerScreenState == TimerState.Finished) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            Text(
                text = formattedTime,
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = formattedTime.calculateFontSize(),
                        fontWeight = FontWeight.Companion.W400,
                        letterSpacing = 1.sp,
                    ),
                color = color,
                modifier =
                    Modifier.Companion.constrainAs(timerText) {
                        linkTo(
                            start = parent.start,
                            top = parent.top,
                            end = parent.end,
                            bottom = parent.bottom,
                        )
                    },
            )

            Button(
                onClick = onOptionTimerClick,
                modifier =
                    Modifier.Companion.constrainAs(optionTimerButton) {
                        linkTo(start = parent.start, end = parent.end)
                        bottom.linkTo(parent.bottom, margin = 20.dp)
                    },
            ) {
                Text(
                    text = "Fix", // Placeholder text, consider making this dynamic or a resource
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Companion.W400,
                        ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
