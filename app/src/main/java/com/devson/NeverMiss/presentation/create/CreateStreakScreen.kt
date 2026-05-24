package com.devson.NeverMiss.presentation.create

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.ui.theme.*

private val CommonEmojis = listOf(
    "🔥", "💪", "💊", "💻", "🦉", "🪴", "🏆", "🔄", "📚", "🍏",
    "💧", "🧠", "🏃", "🧘", "🍳", "🧹", "🐕", "🐈", "🎨", "🎸", "💰", "✨"
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
                title = { Text(if (streakId > 0) "Edit Streak" else "Create Streak", color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = ObsidianBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // LIVE PREVIEW CARD
                Text(
                    text = "LIVE PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Construct a mock streak for preview purposes
                val previewStreak = Streak(
                    title = if (state.title.isEmpty()) "Your Streak Title" else state.title,
                    description = state.description,
                    emoji = state.emoji,
                    category = state.category,
                    color = state.color,
                    creationDate = "",
                    reminderTime = state.reminderTime,
                    reminderRepeatHours = state.reminderRepeatHours,
                    persistent = state.persistent,
                    currentStreak = 0,
                    longestStreak = 0
                )
                
                // Renders the actual card layout used in HomeScreen
                com.devson.NeverMiss.presentation.home.StreakCard(
                    streak = previewStreak,
                    isCompletedToday = false,
                    onClick = {},
                    onLongClick = {},
                    onPause = {},
                    onArchive = {},
                    onDelete = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = AccentCoral,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Title Input
                Text("Streak Title *", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Read coding book, Gym, Watering plants", color = TextSecondaryDark) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = CardSlate,
                        unfocusedContainerColor = CardSlate,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description Input
                Text("Description", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What is the goal of this routine?", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = CardSlate,
                        unfocusedContainerColor = CardSlate,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text("Category", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(StreakCategory.entries.toTypedArray()) { cat ->
                        val isSelected = state.category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else CardSlate)
                                .border(1.dp, if (isSelected) Color.Transparent else BorderSlate, RoundedCornerShape(12.dp))
                                .clickable { viewModel.onCategoryChange(cat) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cat.defaultEmoji} ${cat.displayName.split("/").first()}",
                                color = if (isSelected) Color.White else TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emoji Selection Grid
                Text("Icon / Emoji", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CommonEmojis) { emoji ->
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

                Spacer(modifier = Modifier.height(16.dp))

                // Color Selection
                Text("Theme Color", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(StreakColors) { color ->
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
                                .clickable { viewModel.onColorChange(color.toArgb()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = BorderSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // REMINDER CONFIGURATION
                Text(
                    text = "REMINDER CONFIGURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Time Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = SecondaryTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Exact Reminder Time", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("When to alarm every day", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    val currentHour = state.reminderTime.split(":").first().toIntOrNull() ?: 10
                                    val currentMinute = state.reminderTime.split(":").last().toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.onReminderTimeChange(String.format("%02d:%02d", hour, minute))
                                        },
                                        currentHour,
                                        currentMinute,
                                        true
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(state.reminderTime, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Repeat intervals
                        Text("Repeat Interval Hours", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Triggers alarm every X hours if task is still pending", color = TextSecondaryDark, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val repeatOptions = listOf(0 to "None", 1 to "1h", 2 to "2h", 4 to "4h", 6 to "6h")
                            repeatOptions.forEach { (hours, label) ->
                                val isSelected = state.reminderRepeatHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBg)
                                        .clickable { viewModel.onRepeatHoursChange(hours) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.White else TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Cutoff settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.7f)) {
                                Text("Grace Period (Cutoff until 3 AM)", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Extends streak deadline into early next morning", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                            Switch(
                                checked = state.gracePeriodUntil3AM,
                                onCheckedChange = viewModel::onGracePeriodChange
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.7f)) {
                                Text("Persistent Notification", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Notification pins to status tray until marked done", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                            Switch(
                                checked = state.persistent,
                                onCheckedChange = viewModel::onPersistentChange
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.7f)) {
                                Text("Vibrate on Alert", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Toggle vibration signals when alarms fire", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                            Switch(
                                checked = state.vibrate,
                                onCheckedChange = viewModel::onVibrateChange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save button
                Button(
                    onClick = viewModel::saveStreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (streakId > 0) "Save Changes" else "Start Streak",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
