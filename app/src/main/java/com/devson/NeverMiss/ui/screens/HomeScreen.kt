package com.devson.NeverMiss.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.ui.theme.*
import com.devson.NeverMiss.ui.util.getCategoryIcon
import com.devson.NeverMiss.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// Confetti Particle model
enum class ParticleShape { RECT, CIRCLE, RIBBON }

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val angle: Float,
    val rotationSpeed: Float,
    val alpha: Float = 1f,
    val shape: ParticleShape = ParticleShape.RECT
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

    // Track canvas size so particles spawn at the real center
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    LaunchedEffect(triggerConfetti) {
        if (triggerConfetti) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            val spawnX = if (canvasWidth > 0f) canvasWidth / 2f else 540f
            val spawnY = if (canvasHeight > 0f) canvasHeight * 0.45f else 960f

            val confettiColors = listOf(
                Color(0xFFFFD700), // Gold
                Color(0xFF00E5FF), // Cyan
                Color(0xFF69F0AE), // Green
                Color(0xFFFF4081), // Pink
                Color(0xFFE040FB), // Purple
                Color(0xFFFF6D00), // Orange
                Color(0xFF40C4FF), // Light blue
                Color(0xFFFFFF00)  // Yellow
            )
            val shapes = ParticleShape.entries.toTypedArray()

            val list = (0 until 120).map {
                val speed = Random.nextFloat() * 28f + 12f
                val angleDeg = Random.nextFloat() * 360f
                val rad = Math.toRadians(angleDeg.toDouble())
                Particle(
                    x = spawnX,
                    y = spawnY,
                    vx = (Math.cos(rad) * speed).toFloat(),
                    vy = (Math.sin(rad) * speed - 22f).toFloat(), // bias upward
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 14f + 7f,
                    angle = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 12f,
                    shape = shapes.random()
                )
            }
            confettiParticles = list
            triggerConfetti = false

            // Animation loop — ~140 frames (~2.3 s), fade starts at 60% life
            val totalTicks = 140
            var ticks = 0
            while (confettiParticles.isNotEmpty() && ticks < totalTicks) {
                val progress = ticks.toFloat() / totalTicks
                val fadeStart = 0.55f
                confettiParticles = confettiParticles.mapNotNull { p ->
                    val newAlpha = if (progress < fadeStart) 1f
                    else (1f - (progress - fadeStart) / (1f - fadeStart)).coerceAtLeast(0f)
                    if (newAlpha <= 0.01f || p.y > canvasHeight + 100f) null
                    else p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        vy = p.vy + 0.65f,   // gravity
                        vx = p.vx * 0.985f,  // air friction
                        angle = p.angle + p.rotationSpeed,
                        alpha = newAlpha
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
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(MaterialTheme.colorScheme.background)
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
                            text = if (state.showArchived) "Archived Vault" else "NeverMiss",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (state.showArchived) "Manage your archived streaks" else "Consistency builds greatness",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Stat Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AcUnit, contentDescription = "Freezes", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.freezesRemaining} Freezes",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Date & Time Showcase (Default / Custom Override)
                Spacer(modifier = Modifier.height(10.dp))

                val isOverride = AppClock.isOverrideActive()
                val displayDate = AppClock.now()
                val formatter = remember(displayDate) {
                    DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")
                }
                val dateStr = remember(displayDate, formatter) {
                    displayDate.format(formatter)
                }

                var currentTimeStr by remember { mutableStateOf("") }

                LaunchedEffect(isOverride) {
                    while (true) {
                        val nowTime = LocalTime.now()
                        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                        currentTimeStr = nowTime.format(timeFormatter)
                        delay(1000)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isOverride) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isOverride) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOverride) Icons.Default.EditCalendar else Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isOverride) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateStr,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isOverride) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = currentTimeStr,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search Bar
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = { Text("Search streaks...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(onClick = viewModel::onToggleShowArchived) {
                                Icon(
                                    imageVector = if (state.showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Toggle Archived",
                                    tint = if (state.showArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = viewModel::onToggleLayout) {
                                Icon(
                                    imageVector = if (state.isGridView) Icons.Default.List else Icons.Default.GridView,
                                    contentDescription = "Toggle Layout",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
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
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    items(StreakCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.name,
                            onClick = { viewModel.onCategorySelected(cat.name) },
                            label = { Text(cat.displayName.split("/").first()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Streaks Content
                Spacer(modifier = Modifier.height(16.dp))

                val isListEmpty = if (state.showArchived) state.archivedStreaks.isEmpty() else state.activeStreaks.isEmpty()

                if (isListEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (state.showArchived) Icons.Default.FolderOpen else Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (state.showArchived) "No archived streaks found." else "No streaks created yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (!state.showArchived) {
                                Text("Tap the + button to build consistency!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    val todayStr = StreakCalculator.formatDate(AppClock.now())
                    val displayStreaks = if (state.showArchived) state.archivedStreaks else state.activeStreaks

                    if (state.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayStreaks) { streak ->
                                val isDone = streak.lastCompletedDate == todayStr
                                StreakGridCard(
                                    streak = streak,
                                    isCompletedToday = isDone,
                                    onClick = {
                                        if (!isDone) {
                                            viewModel.completeStreak(streak.id)
                                            triggerConfetti = true
                                        } else {
                                            viewModel.completeStreak(streak.id)
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
                            if (state.showArchived) {
                                item {
                                    Text("Archived Streaks Vault", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                items(state.archivedStreaks) { streak ->
                                    val isDone = streak.lastCompletedDate == todayStr
                                    StreakCard(
                                        streak = streak,
                                        isCompletedToday = isDone,
                                        onClick = {
                                            if (!isDone) {
                                                viewModel.completeStreak(streak.id)
                                                triggerConfetti = true
                                            } else {
                                                viewModel.completeStreak(streak.id)
                                            }
                                        },
                                        onLongClick = { onNavigateToEdit(streak.id) },
                                        onPause = { viewModel.togglePauseStreak(streak) },
                                        onArchive = { viewModel.toggleArchiveStreak(streak) },
                                        onDelete = { viewModel.deleteStreak(streak) }
                                    )
                                }
                            } else {
                                // Section: Pending Today
                                if (state.pendingToday.isNotEmpty()) {
                                    item {
                                        Text("Pending Today", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
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
                                        Text("Completed Today", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
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
                                        Text("Scheduled Days Rest", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
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
            }

            // Confetti Overlay Canvas
            if (confettiParticles.isNotEmpty()) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        canvasWidth = coords.size.width.toFloat()
                        canvasHeight = coords.size.height.toFloat()
                    }
                ) {
                    confettiParticles.forEach { p ->
                        rotate(p.angle, pivot = Offset(p.x, p.y)) {
                            when (p.shape) {
                                ParticleShape.RECT -> drawRect(
                                    color = p.color.copy(alpha = p.alpha),
                                    topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 4f),
                                    size = Size(p.size, p.size / 2f)
                                )
                                ParticleShape.CIRCLE -> drawCircle(
                                    color = p.color.copy(alpha = p.alpha),
                                    radius = p.size / 2.5f,
                                    center = Offset(p.x, p.y)
                                )
                                ParticleShape.RIBBON -> drawRect(
                                    color = p.color.copy(alpha = p.alpha),
                                    topLeft = Offset(p.x - p.size * 0.75f, p.y - p.size / 6f),
                                    size = Size(p.size * 1.5f, p.size / 3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreakGridCard(
    streak: Streak,
    isCompletedToday: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPause: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val streakColor = Color(streak.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isCompletedToday) {
            BorderStroke(1.5.dp, streakColor)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Box {
            // Subtle color tint strip at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(streakColor.copy(alpha = 0.8f), streakColor.copy(alpha = 0.3f))
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 12.dp)
            ) {
                // Top row: emoji + menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(streakColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(streak.emoji, fontSize = 20.sp)
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (streak.isPaused) "Resume" else "Pause", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { onPause(); showMenu = false },
                                leadingIcon = {
                                    Icon(
                                        if (streak.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (streak.isArchived) "Unarchive" else "Archive", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { onArchive(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = AccentCoral) },
                                onClick = { onDelete(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentCoral) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = streak.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Text(
                    text = streak.category.displayName.split("/").first(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Streak count row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Streak",
                        tint = streakColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${streak.currentStreak}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = streakColor
                    )
                    if (streak.longestStreak > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ ${streak.longestStreak}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status badge
                val (badgeText, badgeColor) = when {
                    isCompletedToday -> "DONE" to SuccessGreen
                    streak.isPaused  -> "PAUSED" to MaterialTheme.colorScheme.onSurfaceVariant
                    else             -> "DUE" to WarningAmber
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isCompletedToday) {
            BorderStroke(1.5.dp, Color(streak.color))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = streak.category.displayName.split("/").first(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Streak Number Count
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = "Streak",
                                tint = Color(streak.color),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${streak.currentStreak}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(streak.color)
                            )
                        }
                        if (streak.longestStreak > 0) {
                            Text(
                                text = "Max ${streak.longestStreak}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (streak.isPaused) "Resume" else "Pause", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { onPause(); showMenu = false },
                                leadingIcon = { Icon(if (streak.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (streak.isArchived) "Unarchive" else "Archive", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { onArchive(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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