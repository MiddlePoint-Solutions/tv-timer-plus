package io.middlepoint.tvsleep.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.BuildConfig
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimeSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: TimeSelectionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    TimerSetup(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp),
        timeOptions = uiState.timeOptions,
        onClick = { viewModel.onEvent(TimeSelectionEvent.OnTimeSelected(it)) },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimerSetup(
    timeOptions: List<TimeOptionItem>,
    onClick: (TimeOptionItem) -> Unit,
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
            modifier = Modifier.padding(top = 40.dp),
            style = MaterialTheme.typography.displayLarge,
        )

        Spacer(modifier = Modifier.size(20.dp))

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
                            time = debugTimeOption.time,
                            onClick = { onClick(debugTimeOption) },
                        )
                    }
                }

                items(timeOptions, key = { it.time }) { item ->
                    TimeOption(
                        time = item.time,
                        onClick = { onClick(item) },
                    )
                }

                item {
                    TimeOption(
                        time = "Custom",
                        onClick = { },
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimeOption(
    time: String = "00:00",
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
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
        TimerSetup(timeOptions = emptyList(), onClick = {}) // Preview onClick remains the same, will adapt to new signature
    }
}
