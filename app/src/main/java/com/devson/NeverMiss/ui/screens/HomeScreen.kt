package com.devson.NeverMiss.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.core.util.TrackedAppIcon
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.navigation.Screen
import com.devson.NeverMiss.ui.components.FloatingCapsuleNavigationBar
import com.devson.NeverMiss.ui.screens.appstreaks.AccountConfirmationDialog
import com.devson.NeverMiss.ui.screens.appstreaks.PendingConfirmationBanner
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.WarningAmber
import com.devson.NeverMiss.ui.util.StreakIconHelper
import com.devson.NeverMiss.viewmodel.HomeAppStreakItem
import com.devson.NeverMiss.viewmodel.HomeViewModel
import com.devson.NeverMiss.viewmodel.appstreaks.AccountConfirmationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToStreakDetail: (Long, StreakType) -> Unit = { _, _ -> },
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppStreaks: () -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    confirmationViewModel: AccountConfirmationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val confirmationState by confirmationViewModel.uiState.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }

    var isFabExpanded by remember { mutableStateOf(false) }

    // Intercept back button to close speed-dial FAB when expanded
    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "NeverMiss",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Consistency builds greatness",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToArchive) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archived Streaks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
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
                FloatingCapsuleNavigationBar(
                    currentRoute = Screen.Home.route,
                    onNavigateToRoute = { route ->
                        when (route) {
                            Screen.AppStreaksDashboard.route -> onNavigateToAppStreaks()
                            Screen.Analytics.route -> onNavigateToAnalytics()
                            Screen.Settings.route -> onNavigateToSettings()
                        }
                    }
                )
            },
            floatingActionButton = {
                ExpandableFabMenu(
                    isExpanded = isFabExpanded,
                    onToggle = { isFabExpanded = !isFabExpanded },
                    onNormalClick = {
                        isFabExpanded = false
                        onNavigateToCreate()
                    },
                    onAppStreakClick = {
                        isFabExpanded = false
                        onNavigateToAppStreaks()
                    }
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                ) {
                    // Search Bar
                    item(key = "search_bar") {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search habits & apps...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Pending Confirmation Banner
                    val pendingConfirmations = confirmationState.pendingConfirmations
                    if (pendingConfirmations.isNotEmpty()) {
                        item(key = "pending_confirmation_banner") {
                            PendingConfirmationBanner(
                                count = pendingConfirmations.size,
                                firstAppName = pendingConfirmations.first().app.appName,
                                onClick = { showConfirmationDialog = true }
                            )
                        }
                    }

                    // Section 1: Normal Streaks
                    item(key = "normal_streaks_heading") {
                        SectionHeader(
                            title = "NORMAL STREAKS",
                            count = state.normalStreaks.size
                        )
                    }

                    if (state.normalStreaks.isEmpty()) {
                        item(key = "normal_streaks_empty") {
                            EmptySectionCard(
                                title = "No Habit Streaks Yet",
                                message = "Track daily habits like reading, meditating, or exercising. Tap + to begin.",
                                icon = Icons.Default.SelfImprovement
                            )
                        }
                    } else {
                        items(
                            items = state.normalStreaks,
                            key = { "normal_${it.id}" }
                        ) { streak ->
                            NormalStreakCard(
                                streak = streak,
                                onClick = { onNavigateToStreakDetail(streak.id, StreakType.NORMAL) }
                            )
                        }
                    }

                    // Section 2: App Streaks
                    item(key = "app_streaks_heading") {
                        SectionHeader(
                            title = "APP STREAKS",
                            count = state.appStreaks.size,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    if (state.appStreaks.isEmpty()) {
                        item(key = "app_streaks_empty") {
                            EmptySectionCard(
                                title = "No App Streaks Tracked",
                                message = "Track daily active usage and sub-streaks across your installed applications.",
                                icon = Icons.Default.Apps
                            )
                        }
                    } else {
                        items(
                            items = state.appStreaks,
                            key = { "app_${it.app.id}" }
                        ) { appItem ->
                            AppStreakCard(
                                appItem = appItem,
                                onClick = { onNavigateToStreakDetail(appItem.app.id, StreakType.APP) }
                            )
                        }
                    }
                }
            }
        }

        // Tap-outside scrim for speed-dial FAB
        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isFabExpanded = false
                    }
            )
        }
    }

    // Account Confirmation Dialog
    if (showConfirmationDialog && confirmationState.activeConfirmation != null) {
        AccountConfirmationDialog(
            confirmationDetails = confirmationState.activeConfirmation,
            selectedAccountIds = confirmationState.selectedAccountIds,
            isProcessing = confirmationState.isProcessing,
            onToggleAccount = confirmationViewModel::toggleAccountSelection,
            onConfirm = {
                confirmationViewModel.confirmCurrentSelection()
                showConfirmationDialog = false
            },
            onDismiss = {
                confirmationViewModel.dismissCurrent()
                showConfirmationDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NormalStreakCard(
    streak: Streak,
    onClick: () -> Unit
) {
    val todayStr = com.devson.NeverMiss.domain.model.StreakCalculator.formatDate(
        com.devson.NeverMiss.core.util.AppClock.now()
    )
    val isDoneToday = streak.lastCompletedDate == todayStr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = StreakIconHelper.getIcon(streak.icon),
                        contentDescription = streak.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = streak.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (streak.description.isNotBlank()) {
                    Text(
                        text = streak.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${streak.currentStreak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Completion status badge
            when {
                streak.isPaused -> {
                    StatusBadge(text = "Paused", color = WarningAmber)
                }
                isDoneToday -> {
                    StatusBadge(text = "Done Today", color = SecondaryTeal, icon = Icons.Default.Check)
                }
                else -> {
                    StatusBadge(
                        text = "Pending",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.Schedule
                    )
                }
            }
        }
    }
}

@Composable
private fun AppStreakCard(
    appItem: HomeAppStreakItem,
    onClick: () -> Unit
) {
    val app = appItem.app

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TrackedAppIcon(
                packageName = app.packageName,
                iconSize = 46.dp,
                clipShape = RoundedCornerShape(14.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = appItem.accountSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (appItem.accountCount > 1) {
                            "${appItem.accountCount} sub-streaks"
                        } else {
                            "${app.currentStreak} day streak"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when {
                app.isPaused -> {
                    StatusBadge(text = "Paused", color = WarningAmber)
                }
                appItem.isCompletedToday -> {
                    StatusBadge(text = "Done Today", color = SecondaryTeal, icon = Icons.Default.Check)
                }
                else -> {
                    StatusBadge(text = "Active", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandableFabMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNormalClick: () -> Unit,
    onAppStreakClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: Normal Habit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable(onClick = onNormalClick)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Normal Habit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = onNormalClick,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New Habit"
                        )
                    }
                }

                // Action 2: App Streak
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable(onClick = onAppStreakClick)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "App Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = onAppStreakClick,
                        containerColor = SecondaryTeal.copy(alpha = 0.2f),
                        contentColor = SecondaryTeal,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "New App Streak"
                        )
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close" else "Add Streak",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}
