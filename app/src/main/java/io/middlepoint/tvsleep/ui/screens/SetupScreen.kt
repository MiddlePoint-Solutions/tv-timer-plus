package io.middlepoint.tvsleep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.MainActivityViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: MainActivityViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Setup Required",
            style = MaterialTheme.typography.headlineLarge,
        )
        // Later, this screen will show contextual information based on the error.
    }
}
