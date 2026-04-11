package com.timebox.app.ui.applist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val minuteChoices = listOf(0, 15, 30, 45)

@Composable
fun TimeLimitPickerDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: (limitMs: Long) -> Unit
) {
    var hours by remember { mutableIntStateOf(1) }
    var minuteIndex by remember { mutableIntStateOf(0) }

    val minutes = minuteChoices[minuteIndex]
    val totalMs = (hours * 60L + minutes) * 60_000L
    val valid = totalMs >= 15 * 60_000L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set daily limit for $appName") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Hours (0–23)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hours.toFloat(),
                    onValueChange = { hours = it.toInt().coerceIn(0, 23) },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Text("$hours h", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Minutes", style = MaterialTheme.typography.labelMedium)
                minuteChoices.forEachIndexed { index, m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = minuteIndex == index,
                                onClick = { minuteIndex = index },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = minuteIndex == index,
                            onClick = null
                        )
                        Text("${m}m", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (!valid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Minimum is 15 minutes",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(totalMs) },
                enabled = valid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
