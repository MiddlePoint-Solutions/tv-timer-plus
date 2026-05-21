package io.middlepoint.tvsleep.ui.screens.timer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.TimerState
import io.middlepoint.tvsleep.ui.components.MainTimer
import io.middlepoint.tvsleep.ui.components.V2FocusableCard
import io.middlepoint.tvsleep.ui.theme.V2BackgroundBrush

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerScreenViewModel = viewModel(),
) {
    val timerLabel by viewModel.timerLabel.collectAsState()
    val selectedTimeOptionLabel by viewModel.selectedTimeOptionLabel.collectAsState()
    val timerScreenState by viewModel.timerScreenState.collectAsState()
    val timerProgressOffset by viewModel.timerProgressOffset.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = timerProgressOffset,
        animationSpec = SpringSpec(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = 1 / 1000f,
        ),
        label = "TimerProgressAnimation",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(V2BackgroundBrush),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = selectedTimeOptionLabel,
            fontSize = 36.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.alpha(0.85f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        MainTimer(
            animatedProgress = animatedProgress,
            formattedTime = timerLabel,
            timerScreenState = timerScreenState,
            modifier = Modifier.size(520.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        ActionButtons(
            onActionClick = { viewModel.onActionClick() },
            onDelete = { viewModel.onDelete() },
            timerScreenState = timerScreenState,
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
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        V2FocusableCard(
            onClick = onActionClick,
            modifier = Modifier
                .size(124.dp)
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when (timerScreenState) {
                    is TimerState.Running, TimerState.Started -> Icons.Filled.Pause
                    is TimerState.Start, TimerState.Paused, TimerState.Stopped -> Icons.Filled.PlayArrow
                    TimerState.Finish, TimerState.Finished -> Icons.Filled.Stop
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.label_start),
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        Spacer(Modifier.width(80.dp))

        V2FocusableCard(
            onClick = onDelete,
            modifier = Modifier.size(124.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}
