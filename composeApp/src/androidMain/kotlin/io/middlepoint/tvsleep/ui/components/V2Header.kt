package io.middlepoint.tvsleep.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun V2Header(
    step: Int,
    totalSteps: Int,
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        V2StepDots(
            currentStep = step,
            totalSteps = totalSteps
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            letterSpacing = 0.1.em
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 96.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = (-0.035).em,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
