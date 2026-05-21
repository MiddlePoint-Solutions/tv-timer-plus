package io.middlepoint.tvsleep.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.lightColorScheme
import io.middlepoint.tvsleep.BgGradientBottom
import io.middlepoint.tvsleep.BgGradientTop
import io.middlepoint.tvsleep.Purple40

val V2BackgroundBrush: Brush
    @Composable
    get() = remember {
        Brush.radialGradient(
            colors = listOf(BgGradientTop, Purple40, BgGradientBottom),
            center = Offset.Zero,
            radius = 2000f
        )
    }

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
