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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.ui.theme.*
import com.devson.NeverMiss.ui.util.getCategoryIcon
import com.devson.NeverMiss.viewmodel.CreateStreakViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val CommonEmojis = listOf(
    "🔥", "💪", "💊", "💻", "🦉", "🪴", "🏆", "🔄", "📚", "🍏",
    "💧", "🧠", "🏃", "🧘", "🍳", "🧹", "🎨", "🎸", "💰", "✨"
)

private val WeekDays = listOf(
    1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (streakId > 0) "Edit Streak" else "Create Streak",
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
                .padding(innerPadding),
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

                // SECTION 1: EMOJI & TITLE CARD
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
                            // Selected Emoji Avatar
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(state.color).copy(alpha = 0.18f))
                                    .border(1.5.dp, Color(state.color), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(state.emoji, fontSize = 28.sp)
                            }

                            // Title Field
                            OutlinedTextField(
                                value = state.title,
                                onValueChange = viewModel::onTitleChange,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. Morning Workout", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                label = { Text("Streak Title *") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Emoji Picker Row
                        Text("Select Icon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CommonEmojis, key = { it }) { emoji ->
                                val isSelected = state.emoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                        .clickable { viewModel.onEmojiChange(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }

                        // Color Picker Palette
                        Text("Accent Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StreakColors, key = { it.toArgb() }) { color ->
                                val isSelected = state.color == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            2.dp,
                                            if (isSelected) Color.White else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { viewModel.onColorChange(color.toArgb()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: CATEGORY SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StreakCategory.entries.toTypedArray(), key = { it.name }) { cat ->
                            val isSelected = state.category == cat
                            Surface(
                                selected = isSelected,
                                onClick = { viewModel.onCategoryChange(cat) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cat.displayName.split("/").first(),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: SCHEDULE & DAILY REMINDER TIMER
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
                        Text("SCHEDULE & DAILY REMINDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Days Selector (Everyday vs Custom Days)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Repeat Days", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                FilterChip(
                                    selected = state.daysOfWeek.isEmpty(),
                                    onClick = viewModel::setEveryday,
                                    label = { Text("Everyday") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                WeekDays.forEach { (dayInt, label) ->
                                    val isSelected = state.daysOfWeek.contains(dayInt) || state.daysOfWeek.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            .clickable { viewModel.toggleDayOfWeek(dayInt) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label.first().toString(),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        // Reminder Time Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = SecondaryTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Reminder Timer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Sends daily popup notification", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Formatted Time Button
                            val formattedTime = remember(state.reminderTime) {
                                try {
                                    val parsed = LocalTime.parse(state.reminderTime)
                                    parsed.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                } catch (e: Exception) {
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(formattedTime, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // SECTION: ALREADY HAVE A STREAK? (OPTIONAL)
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
                            text = "STARTING STREAK PROGRESS (OPTIONAL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Already maintaining this habit? Enter your ongoing streak count to continue seamlessly without starting from zero.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = state.initialStreak,
                            onValueChange = viewModel::onInitialStreakChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Already Maintained Streak (Days)") },
                            placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Text("🔥", fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        val currentCount = state.initialStreak.toIntOrNull() ?: 0
                        val previewText = if (currentCount > 0) {
                            "✨ Your streak will start at $currentCount days and advance to ${currentCount + 1} upon today's completion."
                        } else {
                            "Your streak will start at 0 days and advance to 1 upon today's completion."
                        }

                        Text(
                            text = previewText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (currentCount > 0) SecondaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // SECTION 4: DESCRIPTION / GOAL (OPTIONAL)
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description / Goal (Optional)") },
                    placeholder = { Text("Why is this routine important to you?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SAVE BUTTON
                Button(
                    onClick = viewModel::saveStreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (streakId > 0) "Save Changes" else "Start Routine",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
