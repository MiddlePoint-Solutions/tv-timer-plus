package io.middlepoint.tvsleep.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.middlepoint.tvsleep.DashedBorder

fun Modifier.dashedBorder(
    color: Color = DashedBorder,
    strokeWidth: Dp = 2.dp,
    cornerRadius: Dp = 22.dp,
    dashLength: Float = 10f,
    gapLength: Float = 10f
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength))
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
        size = Size(
            size.width - strokeWidth.toPx(),
            size.height - strokeWidth.toPx()
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}
