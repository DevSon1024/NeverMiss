package com.devson.NeverMiss.ui.screens.intervaltimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditIntervalTimerDialog(
    initialTimer: IntervalTimer? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, mode: IntervalTimerMode, durationMillis: Long, intervalMillis: Long, repeatLimit: Int?) -> Unit
) {
    var title by remember { mutableStateOf(initialTimer?.title ?: "") }
    var selectedMode by remember { mutableStateOf(initialTimer?.mode ?: IntervalTimerMode.COUNTDOWN) }

    // Countdown inputs
    val initialDuration = initialTimer?.durationMillis ?: (25 * 60 * 1000L)
    var countHours by remember { mutableStateOf(((initialDuration / 3600000L)).toString().let { if (it == "0") "" else it }) }
    var countMinutes by remember { mutableStateOf((((initialDuration % 3600000L) / 60000L)).toString()) }
    var countSeconds by remember { mutableStateOf((((initialDuration % 60000L) / 1000L)).toString().let { if (it == "0") "" else it }) }

    // Interval inputs
    val initialInterval = initialTimer?.intervalMillis ?: (60 * 60 * 1000L)
    var intHours by remember { mutableStateOf(((initialInterval / 3600000L)).toString().let { if (it == "0") "" else it }) }
    var intMinutes by remember { mutableStateOf((((initialInterval % 3600000L) / 60000L)).toString()) }
    var intSeconds by remember { mutableStateOf((((initialInterval % 60000L) / 1000L)).toString().let { if (it == "0") "" else it }) }

    // Repeat limit
    var isUnlimited by remember { mutableStateOf(initialTimer?.repeatLimit == null) }
    var repeatLimitText by remember { mutableStateOf(initialTimer?.repeatLimit?.toString() ?: "5") }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Text(
                    text = if (initialTimer == null) "Create Interval Timer" else "Edit Interval Timer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Timer Type Switcher (only selectable when creating or if inactive)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == IntervalTimerMode.COUNTDOWN,
                        onClick = { selectedMode = IntervalTimerMode.COUNTDOWN },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Countdown")
                    }
                    SegmentedButton(
                        selected = selectedMode == IntervalTimerMode.INTERVAL,
                        onClick = { selectedMode = IntervalTimerMode.INTERVAL },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Interval")
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Timer Title") },
                    placeholder = { Text(if (selectedMode == IntervalTimerMode.COUNTDOWN) "e.g. Focus Session" else "e.g. Drink Water") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration / Interval Configuration
                if (selectedMode == IntervalTimerMode.COUNTDOWN) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = countHours,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) countHours = it },
                            label = { Text("Hours") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = countMinutes,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) countMinutes = it },
                            label = { Text("Minutes") },
                            placeholder = { Text("25") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = countSeconds,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) countSeconds = it },
                            label = { Text("Seconds") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = "Interval Frequency",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = intHours,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) intHours = it },
                            label = { Text("Hours") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = intMinutes,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) intMinutes = it },
                            label = { Text("Minutes") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = intSeconds,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) intSeconds = it },
                            label = { Text("Seconds") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Repeat limit options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Unlimited Repeats",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isUnlimited,
                            onCheckedChange = { isUnlimited = it }
                        )
                    }

                    if (!isUnlimited) {
                        OutlinedTextField(
                            value = repeatLimitText,
                            onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) repeatLimitText = it },
                            label = { Text("Repeat Limit (times)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Error Display
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            if (cleanTitle.isEmpty()) {
                                validationError = "Please enter a timer title"
                                return@Button
                            }

                            if (selectedMode == IntervalTimerMode.COUNTDOWN) {
                                val h = countHours.toLongOrNull() ?: 0L
                                val m = countMinutes.toLongOrNull() ?: 0L
                                val s = countSeconds.toLongOrNull() ?: 0L
                                val total = (h * 3600 + m * 60 + s) * 1000L
                                if (total <= 0L) {
                                    validationError = "Duration must be greater than 0"
                                    return@Button
                                }
                                onSave(cleanTitle, IntervalTimerMode.COUNTDOWN, total, 0L, null)
                            } else {
                                val h = intHours.toLongOrNull() ?: 0L
                                val m = intMinutes.toLongOrNull() ?: 0L
                                val s = intSeconds.toLongOrNull() ?: 0L
                                val total = (h * 3600 + m * 60 + s) * 1000L
                                if (total <= 0L) {
                                    validationError = "Interval must be greater than 0"
                                    return@Button
                                }
                                val limit = if (isUnlimited) null else repeatLimitText.toIntOrNull()
                                if (!isUnlimited && (limit == null || limit <= 0)) {
                                    validationError = "Repeat limit must be a positive number"
                                    return@Button
                                }
                                onSave(cleanTitle, IntervalTimerMode.INTERVAL, 0L, total, limit)
                            }
                        }
                    ) {
                        Text(if (initialTimer == null) "Create" else "Save")
                    }
                }
            }
        }
    }
}
