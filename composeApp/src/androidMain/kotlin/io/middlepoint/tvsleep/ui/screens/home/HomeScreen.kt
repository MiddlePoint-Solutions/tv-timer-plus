package io.middlepoint.tvsleep.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import io.middlepoint.tvsleep.BuildConfig
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.ui.screens.CustomTimeDialog
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.itemInDeleteMode != null || uiState.selectionMode == SelectionMode.App) {
        if (uiState.selectionMode == SelectionMode.App) {
            viewModel.onEvent(TimeSelectionEvent.OnBackFromAppSelection)
        } else {
            viewModel.onEvent(TimeSelectionEvent.OnCancelDelete)
        }
    }

    if (uiState.showDialog) {
        CustomTimeDialog(
            onDismissRequest = { viewModel.onEvent(TimeSelectionEvent.HideCustomTimeDialog) },
            onSave = { time, label ->
                viewModel.onEvent(TimeSelectionEvent.SaveCustomTime(time, label))
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val title = when (uiState.selectionMode) {
            SelectionMode.Time -> stringResource(R.string.timer_setup_title)
            SelectionMode.App -> stringResource(R.string.app_selection_title)
        }

        Text(
            text = title,
            modifier = Modifier.padding(top = 40.dp),
            style = MaterialTheme.typography.displayLarge,
        )

        Spacer(modifier = Modifier.size(20.dp))

        AnimatedContent(targetState = uiState.selectionMode, label = "Time/App selection") {
            when (it) {
                SelectionMode.Time ->
                    TimerSetup(
                        state = uiState,
                        onEvent = viewModel::onEvent,
                    )

                SelectionMode.App ->
                    AppSelection(
                        state = uiState,
                        onEvent = viewModel::onEvent,
                    )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppSelection(
    state: TimeSelectionState,
    onEvent: (TimeSelectionEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.focusRequester(focusRequester),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true,
        ) {
            item {
                Card(
                    onClick = { onEvent(TimeSelectionEvent.StartTimerOnly) },
                    modifier = Modifier.size(100.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = stringResource(R.string.start_timer_only),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.start_timer_only),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            items(state.installedApps) { app ->
                Card(
                    onClick = { onEvent(TimeSelectionEvent.OnAppSelected(app)) },
                    modifier = Modifier.size(100.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(app.icon),
                            contentDescription = app.label,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = app.label,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimerSetup(
    state: TimeSelectionState,
    onEvent: (TimeSelectionEvent) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.focusRequester(focusRequester),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true,
        ) {
            if (BuildConfig.DEBUG) {
                item {
                    TimeOption(
                        time = "DEBUG",
                        isInDeleteMode = false,
                        isEasterEgg = state.showEasterEgg,
                        onClick = { onEvent(TimeSelectionEvent.OnTimeSelected(debugTimeOption)) },
                        onLongClick = { onEvent(TimeSelectionEvent.ShowEasterEgg) },
                    )
                }
            }

            items(state.timeOptions, key = { it.time }) { item ->
                val isInDeleteMode = state.itemInDeleteMode == item
                TimeOption(
                    time = item.time,
                    isInDeleteMode = isInDeleteMode,
                    isEasterEgg = false,
                    onClick = {
                        if (isInDeleteMode) {
                            onEvent(TimeSelectionEvent.OnDeleteItem(item))
                        } else {
                            onEvent(TimeSelectionEvent.OnTimeSelected(item))
                        }
                    },
                    onLongClick = { onEvent(TimeSelectionEvent.OnTimeItemLongPress(item)) },
                )
            }

            item {
                TimeOption(
                    time = "Custom",
                    isInDeleteMode = false,
                    isEasterEgg = state.showEasterEgg,
                    onClick = { onEvent(TimeSelectionEvent.ShowCustomTimeDialog) },
                    onLongClick = { onEvent(TimeSelectionEvent.ShowEasterEgg) },
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private enum class TimeOptionContentState {
    Normal,
    Delete,
    EasterEgg,
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TimeOption(
    time: String = "00:00",
    isInDeleteMode: Boolean,
    isEasterEgg: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isInDeleteMode) Color.Red else MaterialTheme.colorScheme.primaryContainer,
        label = "Card color",
    )

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.size(100.dp),
        shape = CardDefaults.shape(),
        colors = CardDefaults.colors(containerColor = animatedColor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val contentState =
                when {
                    isEasterEgg -> TimeOptionContentState.EasterEgg
                    isInDeleteMode -> TimeOptionContentState.Delete
                    else -> TimeOptionContentState.Normal
                }
            AnimatedContent(targetState = contentState, label = "Content animation") { state ->
                when (state) {
                    TimeOptionContentState.Normal -> {
                        Text(
                            text = time,
                            modifier = Modifier,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }

                    TimeOptionContentState.Delete -> {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(48.dp),
                        )
                    }

                    TimeOptionContentState.EasterEgg -> {
                        Icon(
                            imageVector = Icons.Default.SentimentVerySatisfied,
                            contentDescription = "Smiley face",
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    TVsleepTheme {
        TimerSetup(
            state = TimeSelectionState(),
            onEvent = {},
        )
    }
}
