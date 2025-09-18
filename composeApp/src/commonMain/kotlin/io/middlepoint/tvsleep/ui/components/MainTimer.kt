package io.middlepoint.tvsleep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.calculateFontSize

@Composable
fun MainTimer(
    animatedProgress: Float,
    timerScreenState: TimerState,
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
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
            val (timerText, optionTimerButton) = createRefs()

            Text(
                text = formattedTime,
                style =
                    MaterialTheme.typography.displaySmall.copy(
                        fontSize = formattedTime.calculateFontSize(),
                        fontWeight = FontWeight.Companion.W300,
                        letterSpacing = 1.sp,
                    ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier =
                    Modifier
                        .constrainAs(timerText) {
                            linkTo(
                                start = parent.start,
                                top = parent.top,
                                end = parent.end,
                                bottom = parent.bottom,
                            )
                        },
            )

//            Button( // TODO: Replace with label of the timer?
//                onClick = {},
//                modifier =
//                    Modifier.constrainAs(optionTimerButton) {
//                        linkTo(start = parent.start, end = parent.end)
//                        bottom.linkTo(parent.bottom, margin = 20.dp)
//                    },
//            ) {
//                Text(
//                    text = "Fix", // Placeholder text, consider making this dynamic or a resource
//                    style =
//                        MaterialTheme.typography.bodyMedium.copy(
//                            letterSpacing = 0.sp,
//                            fontWeight = FontWeight.Companion.W400,
//                        ),
//                    color = MaterialTheme.colorScheme.onPrimary,
//                )
//            }
        }
    }
}
