package com.devson.NeverMiss.ui.screens.appstreaks

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.ui.components.DeleteStreakConfirmDialog
import com.devson.NeverMiss.ui.components.EditAppStreakNameDialog
import com.devson.NeverMiss.ui.components.ExpandableStreakFabOverlay
import com.devson.NeverMiss.ui.components.StreakQuickSettingsSheet
import com.devson.NeverMiss.ui.components.StreakQuickSettingsTarget
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.WarningAmber
import com.devson.NeverMiss.ui.util.StreakIconHelper
import com.devson.NeverMiss.viewmodel.appstreaks.AppStreakDashboardUiState
import com.devson.NeverMiss.viewmodel.appstreaks.AppStreakDashboardViewModel
import com.devson.NeverMiss.viewmodel.appstreaks.TrackedAppSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStreakDashboardScreen(
    initialSection: String = "app",
    onNavigateBack: () -> Unit,
    onNavigateToAppPicker: () -> Unit,
    onNavigateToAppDetail: (Long) -> Unit,
    onNavigateToNormalDetail: (Long) -> Unit = {},
    onNavigateToEditNormal: (Long) -> Unit = {},
    onNavigateToCreateNormal: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: AppStreakDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedSection by rememberSaveable {
        mutableStateOf(if (initialSection == "normal") "normal" else "app")
    }
    var isFabExpanded by remember { mutableStateOf(false) }
    var longPressedTarget by remember { mutableStateOf<StreakQuickSettingsTarget?>(null) }
    var editingAppTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }

    LaunchedEffect(state.actionState.actionError) {
        state.actionState.actionError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "All Streaks",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToDiagnostics) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Diagnostics",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(80.dp)
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                color = MaterialTheme.colorScheme.background
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    DashboardContent(
                        state = state,
                        selectedSection = selectedSection,
                        onSelectSection = { selectedSection = it },
                        onToggleTracking = viewModel::toggleAppTracking,
                        onAppClick = onNavigateToAppDetail,
                        onAppLongClick = { app ->
                            longPressedTarget = StreakQuickSettingsTarget(
                                id = app.id,
                                type = StreakType.APP,
                                name = app.appName,
                                isPaused = app.isPaused,
                                isArchived = app.isArchived
                            )
                        },
                        onNormalClick = onNavigateToNormalDetail,
                        onNormalLongClick = { streak ->
                            longPressedTarget = StreakQuickSettingsTarget(
                                id = streak.id,
                                type = StreakType.NORMAL,
                                name = streak.title,
                                isPaused = streak.isPaused,
                                isArchived = streak.isArchived
                            )
                        },
                        onNavigateToPicker = onNavigateToAppPicker,
                        onNavigateToCreateNormal = onNavigateToCreateNormal,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
            }
        }

        ExpandableStreakFabOverlay(
            isExpanded = isFabExpanded,
            onToggle = { isFabExpanded = !isFabExpanded },
            onDismiss = { isFabExpanded = false },
            onNormalClick = {
                isFabExpanded = false
                onNavigateToCreateNormal()
            },
            onAppStreakClick = {
                isFabExpanded = false
                onNavigateToAppPicker()
            }
        )
    }

    longPressedTarget?.let { target ->
        StreakQuickSettingsSheet(
            target = target,
            onDismiss = { longPressedTarget = null },
            onEdit = {
                longPressedTarget = null
                if (target.type == StreakType.NORMAL) {
                    onNavigateToEditNormal(target.id)
                } else {
                    editingAppTarget = target.id to target.name
                }
            },
            onTogglePause = {
                longPressedTarget = null
                viewModel.togglePause(target.type, target.id)
            },
            onToggleArchive = {
                longPressedTarget = null
                viewModel.toggleArchive(target.type, target.id)
            },
            onDelete = {
                longPressedTarget = null
                viewModel.requestDelete(target.type, target.id)
            }
        )
    }

    editingAppTarget?.let { (appId, currentName) ->
        EditAppStreakNameDialog(
            appId = appId,
            currentName = currentName,
            onDismiss = { editingAppTarget = null },
            onSave = { id, newName ->
                viewModel.updateAppName(id, newName)
                editingAppTarget = null
            }
        )
    }

    if (state.actionState.showDeleteDialog) {
        DeleteStreakConfirmDialog(
            confirmation = state.actionState.deletionConfirmation,
            onDismiss = viewModel::dismissDeleteDialog,
            onConfirm = viewModel::confirmDelete
        )
    }
}

@Composable
private fun DashboardContent(
    state: AppStreakDashboardUiState,
    selectedSection: String,
    onSelectSection: (String) -> Unit,
    onToggleTracking: (Long) -> Unit,
    onAppClick: (Long) -> Unit,
    onAppLongClick: (com.devson.NeverMiss.domain.model.apptracking.TrackedApp) -> Unit,
    onNormalClick: (Long) -> Unit,
    onNormalLongClick: (Streak) -> Unit,
    onNavigateToPicker: () -> Unit,
    onNavigateToCreateNormal: () -> Unit,
    bottomPadding: Dp
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomPadding + 80.dp)
    ) {
        item {
            SectionSwitcherRow(
                selectedSection = selectedSection,
                appCount = state.trackedApps.size,
                normalCount = state.normalStreaks.size,
                onSelectSection = onSelectSection
            )
        }

        if (selectedSection == "normal") {
            item {
                SummaryMetricsRow(
                    totalTracked = state.normalStreaks.size,
                    completedToday = state.normalCompletedTodayCount,
                    highestStreak = state.normalHighestStreak
                )
            }
            if (state.normalStreaks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToCreateNormal),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text("No Normal Habits Yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Tap + to create a daily habit streak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.normalStreaks, key = { "dash_normal_${it.id}" }) { streak ->
                    DashboardNormalStreakCard(
                        streak = streak,
                        onClick = { onNormalClick(streak.id) },
                        onLongClick = { onNormalLongClick(streak) }
                    )
                }
            }
        } else {
            item {
                SummaryMetricsRow(
                    totalTracked = state.totalTracked,
                    completedToday = state.completedTodayCount,
                    highestStreak = state.highestStreak
                )
            }

            if (state.trackedApps.isEmpty()) {
                item {
                    EmptyTrackedAppsCard(onNavigateToPicker = onNavigateToPicker)
                }
            } else {
                items(state.trackedApps, key = { it.app.id }) { summary ->
                    TrackedAppCard(
                        summary = summary,
                        onToggleTracking = { onToggleTracking(summary.app.id) },
                        onClick = { onAppClick(summary.app.id) },
                        onLongClick = { onAppLongClick(summary.app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionSwitcherRow(
    selectedSection: String,
    appCount: Int,
    normalCount: Int,
    onSelectSection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            "app" to "App Streaks ($appCount)",
            "normal" to "Normal Streaks ($normalCount)"
        )
        tabs.forEach { (key, label) ->
            val isSelected = selectedSection == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelectSection(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardNormalStreakCard(
    streak: Streak,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val todayStr = StreakCalculator.formatDate(AppClock.now())
    val isDoneToday = streak.lastCompletedDate == todayStr
    val accentColor = Color(streak.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = StreakIconHelper.getIcon(streak.icon),
                        contentDescription = streak.title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = streak.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${streak.currentStreak}d (Best: ${streak.longestStreak}d)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    streak.isPaused -> WarningAmber.copy(alpha = 0.16f)
                    isDoneToday -> SecondaryTeal.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isDoneToday) Icons.Default.Check else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (streak.isPaused) WarningAmber else if (isDoneToday) SecondaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = when {
                            streak.isPaused -> "Paused"
                            isDoneToday -> "Done"
                            else -> "Pending"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (streak.isPaused) WarningAmber else if (isDoneToday) SecondaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricsRow(
    totalTracked: Int,
    completedToday: Int,
    highestStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = "Tracked",
            value = totalTracked.toString(),
            icon = Icons.Default.Apps,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Today",
            value = completedToday.toString(),
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Best Streak",
            value = "${highestStreak}d",
            icon = Icons.Default.Whatshot,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackedAppCard(
    summary: TrackedAppSummary,
    onToggleTracking: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val app = summary.app

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Avatar
                com.devson.NeverMiss.core.util.TrackedAppIcon(
                    packageName = app.packageName,
                    iconSize = 44.dp,
                    clipShape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = { onToggleTracking() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${app.currentStreak}d",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Best: ${app.longestStreak}d)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sub-accounts count
                if (summary.accountsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${summary.accountsCount} accounts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Today Completion Status Badge
                if (summary.isCompletedToday) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Done",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pending",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTrackedAppsCard(onNavigateToPicker: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToPicker),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Tracked Apps Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to choose apps from your device and start tracking automatic streaks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

