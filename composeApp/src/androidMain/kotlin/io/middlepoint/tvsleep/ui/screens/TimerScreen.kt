package io.middlepoint.tvsleep.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.ui.components.MainTimer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerScreenViewModel = viewModel(),
) {
    val timerLabel by viewModel.timerLabel.collectAsState() // HH:MM:SS
    val selectedTimeOptionLabel by viewModel.selectedTimeOptionLabel.collectAsState() // Custom Label
    val timerScreenState by viewModel.timerScreenState.collectAsState()
    val timerProgressOffset by viewModel.timerProgressOffset.collectAsState()

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (label, timerContent, actionButtons) = createRefs()
        val animatedProgress by animateFloatAsState(
            targetValue = timerProgressOffset,
            animationSpec =
                SpringSpec(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium, // Changed from StiffnessVeryLow
                    visibilityThreshold = 1 / 1000f,
                ),
            label = "TimerProgressAnimation",
        )

        Text(
            text = selectedTimeOptionLabel,
            style = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier.constrainAs(label) {
                    linkTo(
                        start = parent.start,
                        top = parent.top,
                        end = parent.end,
                        bottom = timerContent.top,
                        bottomMargin = 16.dp,
                        topMargin = 16.dp,
                        verticalBias = 0.67f, // Center vertically in the available space
                    )
                },
        )

        MainTimer(
            animatedProgress = animatedProgress,
            formattedTime = timerLabel,
            timerScreenState = timerScreenState,
            modifier =
                Modifier.size(247.dp).constrainAs(timerContent) {
                    linkTo(
                        start = parent.start,
                        top = parent.top,
                        end = parent.end,
                        bottom = parent.bottom, // Changed from actionButtons.top to parent.bottom
                        bottomMargin = 16.dp,
                        topMargin = 16.dp,
                        verticalBias = 0.5f, // Center vertically in the available space
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
                        bottom.linkTo(parent.bottom, margin = 32.dp)
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            onClick = onActionClick,
            modifier =
                Modifier
                    .size(96.dp)
                    .focusRequester(focusRequester),
            shape = CardDefaults.shape(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val icon =
                    when (timerScreenState) {
                        is TimerState.Running, TimerState.Started -> Icons.Filled.Pause
                        is TimerState.Start, TimerState.Paused, TimerState.Stopped -> Icons.Filled.PlayArrow
                        TimerState.Finish, TimerState.Finished -> Icons.Filled.Stop
                    }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.label_start),
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.width(80.dp))

        Card(
            onClick = onDelete,
            modifier =
                Modifier
                    .size(96.dp),
            shape = CardDefaults.shape(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}
