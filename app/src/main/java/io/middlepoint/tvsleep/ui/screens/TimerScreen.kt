package io.middlepoint.tvsleep.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.ui.components.MainTimer
import io.middlepoint.tvsleep.utils.TimerState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerScreenViewModel = viewModel(),
) {
    val timerLabel by viewModel.timerLabel.collectAsState()
    val timerScreenState by viewModel.timerScreenState.collectAsState()
    val timerProgressOffset by viewModel.timerProgressOffset.collectAsState()

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (actionButtons, timer) = createRefs()
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

        MainTimer(
            animatedProgress = animatedProgress,
            formattedTime = timerLabel,
            timerScreenState = timerScreenState,
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
        )

        ActionButtons(
            onActionClick = { viewModel.onActionClick() },
            onDelete = { viewModel.onDelete() },
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButtons(
    timerScreenState: TimerState,
    onActionClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("ktlint:standard:no-consecutive-comments")
    ConstraintLayout(modifier) {
        val (action, delete) = createRefs()
        IconButton(
            onClick = onActionClick,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
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
                contentDescription = stringResource(R.string.label_start),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Button(
            onClick = onDelete,
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
    }
}
