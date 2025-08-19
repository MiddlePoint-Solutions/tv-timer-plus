package io.middlepoint.tvsleep.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.MainActivityViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StartScreen(
    viewModel: MainActivityViewModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
    }

    LaunchedEffect(Unit) { viewModel.startADBServer() }
}
