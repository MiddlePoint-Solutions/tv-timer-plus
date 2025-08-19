package io.middlepoint.tvsleep.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import io.middlepoint.tvsleep.MainActivityViewModel
import io.middlepoint.tvsleep.MainScreenBody
import io.middlepoint.tvsleep.utils.TimerState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainActivityViewModel,
    modifier: Modifier = Modifier,
) {
    val timerState by viewModel.timerState.observeAsState(TimerState.Stopped)
    val timerTick by viewModel.tick.observeAsState(0L)
    val timerLabel by viewModel.timerLabel.observeAsState("")

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        MainScreenBody(
            time = 30000,
            tick = timerTick,
            timerLabel = timerLabel,
            timerScreenState = timerState,
            timerVisibility = true,
            onActionClick = {},
            onDelete = {},
            onOptionTimerClick = {},
        )
    }
}
