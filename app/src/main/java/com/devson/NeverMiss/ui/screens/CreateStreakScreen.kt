package com.devson.NeverMiss.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.ui.theme.AccentCoral
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.StreakColors
import com.devson.NeverMiss.ui.util.StreakIconHelper
import com.devson.NeverMiss.viewmodel.CreateStreakViewModel
import com.devson.NeverMiss.viewmodel.MAX_STREAK_DESCRIPTION_LENGTH
import com.devson.NeverMiss.viewmodel.MAX_STREAK_TITLE_LENGTH
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStreakScreen(
    streakId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: CreateStreakViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showCustomColorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(streakId) {
        if (streakId > 0) {
            viewModel.loadStreak(streakId)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    val selectedColor = remember(state.color) { Color(state.color) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (streakId > 0) "Edit Habit" else "New Normal Habit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Error Notification
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.errorMessage?.let { msg ->
                        Surface(
                            color = AccentCoral.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentCoral.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = AccentCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // SECTION 1: NAME & ICON
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, selectedColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = StreakIconHelper.getIcon(state.icon),
                                    contentDescription = "Selected Icon",
                                    tint = selectedColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            OutlinedTextField(
                                value = state.title,
                                onValueChange = viewModel::onTitleChange,
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "e.g. Daily Reading",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                label = { Text("Name *") },
                                isError = state.titleError != null,
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = state.titleError ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${state.title.length}/$MAX_STREAK_TITLE_LENGTH",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        Text(
                            text = "CHOOSE ICON",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StreakIconHelper.icons, key = { it.key }) { item ->
                                val isSelected = state.icon.equals(item.key, ignoreCase = true)
                                Surface(
                                    selected = isSelected,
                                    onClick = { viewModel.onIconChange(item.key) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) selectedColor else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = item.label,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: CHOOSE COLOR (PRESETS + CUSTOM HEX PICKER)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHOOSE COLOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("#%06X", 0xFFFFFF and state.color),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = selectedColor
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StreakColors) { presetColor ->
                                val presetArgb = presetColor.toArgb()
                                val isSelected = state.color == presetArgb
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(presetColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.onColorChange(presetArgb) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            item(key = "custom_color_picker") {
                                val isCustom = StreakColors.none { it.toArgb() == state.color }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isCustom) selectedColor else MaterialTheme.colorScheme.background)
                                        .border(
                                            width = if (isCustom) 2.5.dp else 1.dp,
                                            color = if (isCustom) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { showCustomColorDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Colorize,
                                        contentDescription = "Custom Hex Color",
                                        tint = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: DESCRIPTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DESCRIPTION (OPTIONAL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Why is this habit important to you?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            minLines = 2,
                            maxLines = 4,
                            supportingText = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    Text(
                                        text = "${state.description.length}/$MAX_STREAK_DESCRIPTION_LENGTH",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = selectedColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // SECTION 4: DAILY REMINDER (TIME CLOCK PICKER ONLY)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DAILY REMINDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = SecondaryTeal
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Reminder Time",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Runs everyday at your chosen time",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val formattedTime = remember(state.reminderTime) {
                                try {
                                    val parsed = LocalTime.parse(state.reminderTime)
                                    parsed.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                } catch (_: Exception) {
                                    state.reminderTime
                                }
                            }

                            Button(
                                onClick = {
                                    val currentHour = state.reminderTime.split(":").firstOrNull()?.toIntOrNull() ?: 10
                                    val currentMin = state.reminderTime.split(":").lastOrNull()?.toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.onReminderTimeChange(String.format("%02d:%02d", hour, minute))
                                        },
                                        currentHour,
                                        currentMin,
                                        false
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = formattedTime,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SAVE BUTTON
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.saveStreak()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor,
                        disabledContainerColor = selectedColor.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (streakId > 0) "Save Changes" else "Create Habit",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCustomColorDialog) {
        CustomHexColorPickerDialog(
            initialColor = state.color,
            onDismiss = { showCustomColorDialog = false },
            onColorSelected = { argb ->
                viewModel.onColorChange(argb)
                showCustomColorDialog = false
            }
        )
    }
}

@Composable
private fun CustomHexColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val initialCompose = Color(initialColor)
    var red by remember { mutableFloatStateOf(initialCompose.red) }
    var green by remember { mutableFloatStateOf(initialCompose.green) }
    var blue by remember { mutableFloatStateOf(initialCompose.blue) }
    var hexInput by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and initialColor)) }

    val previewColor = Color(red, green, blue, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color Picker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                )

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(7)
                        hexInput = cleaned
                        val hexDigits = cleaned.removePrefix("#")
                        if (hexDigits.length == 6) {
                            try {
                                val parsed = android.graphics.Color.parseColor("#$hexDigits")
                                val c = Color(parsed)
                                red = c.red
                                green = c.green
                                blue = c.blue
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                    },
                    label = { Text("Hex Code (#RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Red: ${(red * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = red,
                        onValueChange = {
                            red = it
                            hexInput = String.format("#%06X", 0xFFFFFF and Color(red, green, blue, 1f).toArgb())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                    )
                    Text("Green: ${(green * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = green,
                        onValueChange = {
                            green = it
                            hexInput = String.format("#%06X", 0xFFFFFF and Color(red, green, blue, 1f).toArgb())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                    Text("Blue: ${(blue * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = blue,
                        onValueChange = {
                            blue = it
                            hexInput = String.format("#%06X", 0xFFFFFF and Color(red, green, blue, 1f).toArgb())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(previewColor.toArgb()) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
