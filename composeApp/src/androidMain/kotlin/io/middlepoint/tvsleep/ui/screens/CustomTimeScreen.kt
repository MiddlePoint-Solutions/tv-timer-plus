package io.middlepoint.tvsleep.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.R

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CustomTimeScreen(
  onSave: (timeInMinutes: Int) -> Unit,
  onBack: () -> Unit
) {

  var selectedTime by remember { mutableStateOf("") }
  val minutes = selectedTime.toIntOrNull() ?: 0

  BackHandler {
    onBack()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    AnimatedVisibility(visible = minutes > 60) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = formatMinutes(minutes),
          style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
    AnimatedContent(
      targetState = selectedTime,
      label = "TimerText",
      transitionSpec = {
        fadeIn(animationSpec = tween(220, delayMillis = 90))
          .togetherWith(fadeOut(animationSpec = tween(90)))
      },
      contentAlignment = Alignment.Center
    ) { time ->
      Text(
        text = time.ifEmpty { stringResource(R.string.enter_time_in_minutes) },
        style = MaterialTheme.typography.displayMedium
      )
    }
    Spacer(modifier = Modifier.height(32.dp))
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 1..3) {
          NumberButton(number = i, onClick = { selectedTime += i })
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 4..6) {
          NumberButton(number = i, onClick = { selectedTime += i })
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 7..9) {
          NumberButton(number = i, onClick = { selectedTime += i })
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberButton(number = 0, onClick = { selectedTime += 0 })
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

      Card(onClick = { if (selectedTime.isNotEmpty()) selectedTime = "" }) {
        Text(
          text = stringResource(R.string.clear),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
      }

      Card(onClick = {
        if (selectedTime.isNotEmpty()) {
          onSave(selectedTime.toInt())
        }
      }) {
        Text(
          text = stringResource(R.string.save),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
      }
    }
  }
}

private fun formatMinutes(minutes: Int): String {
  val hours = minutes / 60
  val remainingMinutes = minutes % 60
  return when {
    hours > 0 && remainingMinutes > 0 -> "$hours hour${if (hours > 1) "s" else ""} and $remainingMinutes minute${if (remainingMinutes > 1) "s" else ""}"
    hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
    else -> "$remainingMinutes minute${if (remainingMinutes > 1) "s" else ""}"
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumberButton(
  number: Int,
  onClick: () -> Unit
) {
  Card(onClick = onClick) {
    Text(
      text = number.toString(),
      modifier = Modifier
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .width(20.dp),
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
  }
}
