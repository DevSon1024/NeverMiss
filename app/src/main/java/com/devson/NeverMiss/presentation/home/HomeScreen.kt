package com.devson.NeverMiss.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.random.Random

// Confetti Particle model
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val angle: Float,
    val rotationSpeed: Float,
    val alpha: Float = 1f
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Confetti Animation State
    var confettiParticles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var triggerConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(triggerConfetti) {
        if (triggerConfetti) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Spawn particles
            val list = mutableListOf<Particle>()
            val colors = listOf(Color.Yellow, Color.Cyan, Color.Green, Color.Magenta, Color.Red, Color.Blue)
            for (i in 0..100) {
                list.add(
                    Particle(
                        x = 500f, // Center relative coordinates updated in Canvas
                        y = 1000f,
                        vx = (Random.nextFloat() - 0.5f) * 30f,
                        vy = -Random.nextFloat() * 40f - 10f,
                        color = colors.random(),
                        size = Random.nextFloat() * 15f + 10f,
                        angle = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
                    )
                )
            }
            confettiParticles = list
            triggerConfetti = false

            // Animation Loop
            var ticks = 0
            while (confettiParticles.isNotEmpty() && ticks < 100) {
                confettiParticles = confettiParticles.mapNotNull { p ->
                    if (p.y > 2200f || p.alpha <= 0.05f) null
                    else p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        vy = p.vy + 0.8f, // Gravity
                        vx = p.vx * 0.98f, // Friction
                        angle = p.angle + p.rotationSpeed,
                        alpha = (p.alpha - 0.01f).coerceAtLeast(0f)
                    )
                }
                delay(16)
                ticks++
            }
            confettiParticles = emptyList()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Streak", modifier = Modifier.size(32.dp))
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = ObsidianBg,
                contentColor = TextSecondaryDark,
                modifier = Modifier.height(72.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Already here */ }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NeverMiss",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Consistency builds greatness",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                    }

                    // Stat Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSlate)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AcUnit, contentDescription = "Freezes", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.freezesRemaining} Freezes",
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Search Bar
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = { Text("Search streaks...", color = TextSecondaryDark) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondaryDark) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::onToggleLayout) {
                            Icon(
                                imageVector = if (state.isGridView) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle Layout",
                                tint = TextSecondaryDark
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = CardSlate,
                        unfocusedContainerColor = CardSlate,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                // Categories Row
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.onCategorySelected(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = CardSlate,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                    items(StreakCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.name,
                            onClick = { viewModel.onCategorySelected(cat.name) },
                            label = { Text("${cat.defaultEmoji} ${cat.displayName.split("/").first()}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = CardSlate,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }

                // Streaks Content
                Spacer(modifier = Modifier.height(16.dp))

                if (state.activeStreaks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✨", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No streaks created yet.", color = TextSecondaryDark, textAlign = TextAlign.Center)
                            Text("Tap the + button to build consistency!", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    val todayStr = StreakCalculator.formatDate(LocalDate.now())
                    
                    if (state.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.activeStreaks) { streak ->
                                val isDone = streak.lastCompletedDate == todayStr
                                StreakCard(
                                    streak = streak,
                                    isCompletedToday = isDone,
                                    onClick = { 
                                        if (!isDone) {
                                            viewModel.completeStreak(streak.id)
                                            triggerConfetti = true
                                        } else {
                                            viewModel.completeStreak(streak.id) // undo completion
                                        }
                                    },
                                    onLongClick = { onNavigateToEdit(streak.id) },
                                    onPause = { viewModel.togglePauseStreak(streak) },
                                    onArchive = { viewModel.toggleArchiveStreak(streak) },
                                    onDelete = { viewModel.deleteStreak(streak) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section: Pending Today
                            if (state.pendingToday.isNotEmpty()) {
                                item {
                                    Text("Pending Today", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                items(state.pendingToday) { streak ->
                                    StreakCard(
                                        streak = streak,
                                        isCompletedToday = false,
                                        onClick = { 
                                            viewModel.completeStreak(streak.id)
                                            triggerConfetti = true
                                        },
                                        onLongClick = { onNavigateToEdit(streak.id) },
                                        onPause = { viewModel.togglePauseStreak(streak) },
                                        onArchive = { viewModel.toggleArchiveStreak(streak) },
                                        onDelete = { viewModel.deleteStreak(streak) }
                                    )
                                }
                            }

                            // Section: Completed Today
                            if (state.completedToday.isNotEmpty()) {
                                item {
                                    Text("Completed Today", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                items(state.completedToday) { streak ->
                                    StreakCard(
                                        streak = streak,
                                        isCompletedToday = true,
                                        onClick = { viewModel.completeStreak(streak.id) },
                                        onLongClick = { onNavigateToEdit(streak.id) },
                                        onPause = { viewModel.togglePauseStreak(streak) },
                                        onArchive = { viewModel.toggleArchiveStreak(streak) },
                                        onDelete = { viewModel.deleteStreak(streak) }
                                    )
                                }
                            }

                            // Section: All Other Active
                            val otherStreaks = state.activeStreaks.filter {
                                it.lastCompletedDate != todayStr && !state.pendingToday.any { p -> p.id == it.id }
                            }
                            if (otherStreaks.isNotEmpty()) {
                                item {
                                    Text("Scheduled Days Rest", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                items(otherStreaks) { streak ->
                                    StreakCard(
                                        streak = streak,
                                        isCompletedToday = false,
                                        onClick = { 
                                            viewModel.completeStreak(streak.id)
                                            triggerConfetti = true
                                        },
                                        onLongClick = { onNavigateToEdit(streak.id) },
                                        onPause = { viewModel.togglePauseStreak(streak) },
                                        onArchive = { viewModel.toggleArchiveStreak(streak) },
                                        onDelete = { viewModel.deleteStreak(streak) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confetti Overlay Canvas
            if (confettiParticles.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    confettiParticles.forEach { p ->
                        // Dynamically resolve start positions to coordinates
                        val finalX = if (p.x == 500f) size.width / 2f + p.vx else p.x
                        val finalY = if (p.y == 1000f) size.height * 0.7f + p.vy else p.y
                        
                        rotate(p.angle, pivot = Offset(finalX, finalY)) {
                            drawRect(
                                color = p.color.copy(alpha = p.alpha),
                                topLeft = Offset(finalX - p.size / 2f, finalY - p.size / 2f),
                                size = androidx.compose.ui.geometry.Size(p.size, p.size)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreakCard(
    streak: Streak,
    isCompletedToday: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPause: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = if (isCompletedToday) {
            androidx.compose.foundation.BorderStroke(1.5.dp, Color(streak.color))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(streak.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(streak.emoji, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                        Text(
                            text = streak.title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = streak.category.displayName.split("/").first(),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Streak Number Count
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "🔥 ${streak.currentStreak}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(streak.color)
                        )
                        if (streak.longestStreak > 0) {
                            Text(
                                text = "Max ${streak.longestStreak}",
                                fontSize = 9.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondaryDark)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(CardSlate)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (streak.isPaused) "Resume" else "Pause", color = TextPrimaryDark) },
                                onClick = { onPause(); showMenu = false },
                                leadingIcon = { Icon(if (streak.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = TextSecondaryDark) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (streak.isArchived) "Unarchive" else "Archive", color = TextPrimaryDark) },
                                onClick = { onArchive(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = TextSecondaryDark) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = AccentCoral) },
                                onClick = { onDelete(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentCoral) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion Progress Bar or Alert Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val nextRemStr = if (streak.isPaused) "Paused" else "Remind at ${streak.reminderTime}"
                Text(
                    text = nextRemStr,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                if (isCompletedToday) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMPLETED",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (streak.isPaused) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TextSecondaryDark.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DUE TODAY",
                            color = WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
