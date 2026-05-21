package io.middlepoint.tvsleep.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import io.middlepoint.tvsleep.FocusRing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun V2FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isFocused) (-2).dp else 0.dp,
        label = "FocusLift"
    )

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.offset(y = offsetY),
        shape = CardDefaults.shape(shape),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, FocusRing))
        ),
        interactionSource = interactionSource,
        content = { content() }
    )
}
