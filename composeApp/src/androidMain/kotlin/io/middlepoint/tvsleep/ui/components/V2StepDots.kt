package io.middlepoint.tvsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun V2StepDots(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            Box(
                modifier = Modifier
                    .width(if (isActive) 24.dp else 12.dp)
                    .height(6.dp)
                    .alpha(if (isActive) 1f else 0.4f)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
