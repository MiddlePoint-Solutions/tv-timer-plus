package io.middlepoint.tvsleep.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.MainActivityViewModel
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.ui.components.CircularProgressWithThumb
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme
import io.middlepoint.tvsleep.utils.TimerState
import io.middlepoint.tvsleep.utils.calculateFontSize
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class TimeOptionItem(
    val time: String,
    val label: String,
    val timeInMillis: Long,
)

private val timeOptions =
    listOf(
        TimeOptionItem("15 Min", "Short distraction", 15.minutes.inWholeMilliseconds),
        TimeOptionItem("1 Hour", "1 hour of peace", 1.hours.inWholeMilliseconds),
        TimeOptionItem("1.5Hours", "Movie", (1.hours + 30.minutes).inWholeMilliseconds), // Corrected 1.5 hours
        TimeOptionItem("2 Hours", "Long Movie", 2.hours.inWholeMilliseconds),
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainActivityViewModel = viewModel(),
) {
    val timerState by viewModel.timerState.observeAsState(TimerState.Stopped)
    val timerTick by viewModel.tick.observeAsState(0L)
    val timerLabel by viewModel.timerLabel.observeAsState("")
    val currentTimerTotalDuration by viewModel.currentTimerTotalDuration.observeAsState(0L)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            visible = timerState == TimerState.Stopped,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TimerSetup(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                onClick = { timeInMillis ->
                    viewModel.onTimeSelected(timeInMillis)
                },
            )
        }

        AnimatedVisibility(
            visible = timerState != TimerState.Stopped,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            MainScreenBody(
                time = currentTimerTotalDuration,
                tick = timerTick,
                timerLabel = timerLabel,
                timerScreenState = timerState,
                onActionClick = { viewModel.togglePlayPause() },
                onDelete = { viewModel.stopTimerAndGoToSetup() },
                onOptionTimerClick = { viewModel.handleMainScreenOptionButtonPressed() },
            )
        }
    }
}

@Composable
private fun TimerSetup(
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val focusRequester = remember { FocusRequester() }

        Text(
            text = stringResource(R.string.timer_setup_title),
            style = MaterialTheme.typography.displayLarge,
        )

        Spacer(modifier = Modifier.size(20.dp))

        Box(
            modifier = Modifier.fillMaxSize(), // Adjusted to fill width and allow centering
            contentAlignment = Alignment.Center,
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(4), // Changed to 2 for better fit with new card size
                contentPadding = PaddingValues(16.dp), // Reduced padding
                modifier = Modifier.focusRequester(focusRequester),
                verticalItemSpacing = 16.dp, // Reduced spacing
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Reduced spacing
                userScrollEnabled = true, // Typically should be true for scrollable content
            ) {
                items(timeOptions) { item ->
                    TimeOption(
                        time = item.time,
                        onClick = { onClick(item.timeInMillis) },
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun TimeOption(
    time: String = "00:00",
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(100.dp), // Adjusted size
        shape = CardDefaults.shape(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = time,
                modifier = Modifier,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    TVsleepTheme {
        // Previewing with TimerSetup as it's the initial state
        TimerSetup(onClick = {})
    }
}

@Composable
private fun MainScreenBody(
    time: Long,
    tick: Long,
    timerLabel: String,
    timerScreenState: TimerState,
    onActionClick: () -> Unit,
    onDelete: () -> Unit,
    onOptionTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (actionButtons, timer) = createRefs()
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
            // timerVisibility removed as visibility is now controlled by higher level AnimatedVisibility and internal logic
            modifier =
                Modifier
                    .size(200.dp)
                    .constrainAs(timer) {
                        linkTo(
                            start = parent.start,
                            top = parent.top,
                            end = parent.end,
                            bottom = parent.bottom,
                            bottomMargin = 16.dp,
                        )
                    },
            onOptionTimerClick = onOptionTimerClick,
        )

        ActionButtons(
            onActionClick = onActionClick,
            onDelete = onDelete,
            timerScreenState = timerScreenState,
            modifier =
                Modifier
                    .constrainAs(actionButtons) {
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        linkTo(
                            start = parent.start,
                            end = parent.end,
                        )
                        width = Dimension.fillToConstraints
                    },
        )
    }
}

@Composable
private fun MainTimer(
    animatedProgress: Float,
    timerScreenState: TimerState,
    formattedTime: String,
    onOptionTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Progress bar is visible unless timer is Finished
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

            val color =
                if (timerScreenState == TimerState.Finished) {
                    MaterialTheme.colorScheme.primary // Highlight finished state
                } else {
                    MaterialTheme.colorScheme.onSurface // Standard text color on surface
                }
            Text(
                text = formattedTime,
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = formattedTime.calculateFontSize(),
                        fontWeight = FontWeight.W400,
                        letterSpacing = 1.sp,
                    ),
                color = color,
                modifier =
                    Modifier.constrainAs(timerText) {
                        linkTo(
                            start = parent.start,
                            top = parent.top,
                            end = parent.end,
                            bottom = parent.bottom, // Center the text
                        )
                    },
            )

            // Option button (e.g., "+1 Min" or "Reset")
            Button(
                onClick = onOptionTimerClick,
                modifier =
                    Modifier.constrainAs(optionTimerButton) {
                        linkTo(start = parent.start, end = parent.end)
                        bottom.linkTo(parent.bottom, margin = 20.dp) // Position below timer text
                    },
            ) {
//                val resId =
//                    when (timerScreenState) {
//                        TimerState.Started, TimerState.Finished -> R.string.label_plus_one_minute
//                        TimerState.Paused, TimerState.Stopped -> R.string.label_reset
//                    }
                Text(
//                    text = stringResource(resId),
                    text = "Fix",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.W400,
                        ),
                    color = MaterialTheme.colorScheme.onPrimary, // Assuming Button uses primary container color
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    timerScreenState: TimerState,
    onActionClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("ktlint:standard:no-consecutive-comments")
    ConstraintLayout(modifier) {
        val (action, delete, addTimer) = createRefs()
        IconButton(
            onClick = onActionClick,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer, // Updated color
                        shape = CircleShape,
                    ).constrainAs(action) {
                        linkTo(
                            start = parent.start,
                            end = parent.end,
                        )
                    },
        ) {
            val icon =
                when (timerScreenState) {
                    is TimerState.Running, TimerState.Started -> Icons.Outlined.Pause
                    is TimerState.Start, TimerState.Paused, TimerState.Stopped -> Icons.Outlined.PlayArrow
                    TimerState.Finish, TimerState.Finished -> Icons.Outlined.Stop
                }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.label_start), // Content description could be more dynamic
                tint = MaterialTheme.colorScheme.onPrimaryContainer, // Updated color
            )
        }
        Button(
            onClick = onDelete, // This will call viewModel.stopTimerAndGoToSetup()
            modifier =
                Modifier.constrainAs(delete) {
                    linkTo(top = action.top, bottom = action.bottom)
                    start.linkTo(parent.start, margin = 16.dp)
                },
        ) {
            Text(
                text = stringResource(R.string.label_delete),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400,
                    ),
            )
        }
        // "Add Timer" button remains commented out as its functionality is not yet defined
        /*
        Button(
            onClick = {}, // Would be something like onAddTimerClick
            enabled = false, // Or determined by some logic
            modifier =
                Modifier
                    .constrainAs(addTimer) {
                        linkTo(top = action.top, bottom = action.bottom)
                        end.linkTo(parent.end, margin = 16.dp)
                    },
        ) {
            Text(
                text = stringResource(R.string.label_add_timer),
                // color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), // Handled by Button's disabled state
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400,
                    ),
            )
        }
         */
    }
}
