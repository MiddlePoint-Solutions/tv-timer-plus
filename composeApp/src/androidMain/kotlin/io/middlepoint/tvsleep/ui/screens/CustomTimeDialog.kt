package io.middlepoint.tvsleep.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CustomTimeDialog(
    onDismissRequest: () -> Unit,
    onSave: (timeInMinutes: String, label: String) -> Unit,
) {
    var timeInMinutes by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Custom Time") },
        text = {
            Column {
                TextField(
                    value = timeInMinutes,
                    onValueChange = { timeInMinutes = it },
                    label = { Text("Time in minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (timeInMinutes.isNotBlank() && label.isNotBlank()) {
                        onSave(timeInMinutes, label)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
