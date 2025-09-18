package io.middlepoint.tvsleep.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.middlepoint.tvsleep.Purple40

@Composable
fun TVsleepTheme(content: @Composable () -> Unit) {
    val colorScheme =
        remember {
            lightColorScheme(
                onPrimary = Color.White,
                background = Purple40,
                onBackground = Color.White,
            )
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
