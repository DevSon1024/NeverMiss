package com.devson.NeverMiss.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.core.util.TrackedAppIcon
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.ui.screens.appstreaks.AccountConfirmationDialog
import com.devson.NeverMiss.ui.screens.appstreaks.PendingConfirmationBanner
import com.devson.NeverMiss.ui.theme.AccentCoral
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.SuccessGreen
import com.devson.NeverMiss.ui.theme.WarningAmber
import com.devson.NeverMiss.ui.util.StreakIconHelper
import com.devson.NeverMiss.viewmodel.CardCompletionStatus
import com.devson.NeverMiss.viewmodel.HomeAppStreakItem
import com.devson.NeverMiss.viewmodel.HomeViewModel
import com.devson.NeverMiss.viewmodel.appstreaks.AccountConfirmationViewModel

private sealed interface LongPressStreakTarget {
    data class Normal(val streak: Streak) : LongPressStreakTarget
    data class App(val item: HomeAppStreakItem) : LongPressStreakTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToStreakDetail: (Long, StreakType) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit,
    onNavigateToAppStreaks: () -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    confirmationViewModel: AccountConfirmationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val confirmationState by confirmationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var longPressedTarget by remember { mutableStateOf<LongPressStreakTarget?>(null) }
    var editingAppTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }

    LaunchedEffect(state.actionDialogState.actionError) {
        state.actionDialogState.actionError?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

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
                        IconButton(onClick = viewModel::toggleGridLayout) {
                            Icon(
                                imageVector = if (state.isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = if (state.isGridLayout) "Switch to List View" else "Switch to Horizontal Grid",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 10.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
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
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                PendingConfirmationBanner(
                                    count = pendingConfirmations.size,
                                    firstAppName = pendingConfirmations.first().app.appName,
                                    onClick = { showConfirmationDialog = true }
                                )
                            }
                        }
                    }

                    // Section 1: Normal Streaks
                    item(key = "normal_streaks_heading") {
                        SectionHeader(
                            title = "NORMAL STREAKS",
                            count = state.normalStreaks.size,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    if (state.normalStreaks.isEmpty()) {
                        item(key = "normal_streaks_empty") {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                EmptySectionCard(
                                    title = "No Habit Streaks Yet",
                                    message = "Track daily habits like reading, meditating, or exercising. Tap + to begin.",
                                    icon = Icons.Default.SelfImprovement
                                )
                            }
                        }
                    } else if (state.isGridLayout) {
                        item(key = "normal_streaks_grid_row") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = state.normalStreaks,
                                    key = { "normal_grid_${it.id}" }
                                ) { streak ->
                                    NormalStreakGridCard(
                                        streak = streak,
                                        onClick = { onNavigateToStreakDetail(streak.id, StreakType.NORMAL) },
                                        onLongClick = { longPressedTarget = LongPressStreakTarget.Normal(streak) }
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = state.normalStreaks,
                            key = { "normal_${it.id}" }
                        ) { streak ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                NormalStreakCard(
                                    streak = streak,
                                    onClick = { onNavigateToStreakDetail(streak.id, StreakType.NORMAL) },
                                    onLongClick = { longPressedTarget = LongPressStreakTarget.Normal(streak) }
                                )
                            }
                        }
                    }

                    // Section 2: App Streaks
                    item(key = "app_streaks_heading") {
                        SectionHeader(
                            title = "APP STREAKS",
                            count = state.appStreaks.size,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 10.dp)
                        )
                    }

                    if (state.appStreaks.isEmpty()) {
                        item(key = "app_streaks_empty") {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                EmptySectionCard(
                                    title = "No App Streaks Tracked",
                                    message = "Track daily active usage and sub-streaks across your installed applications.",
                                    icon = Icons.Default.Apps
                                )
                            }
                        }
                    } else if (state.isGridLayout) {
                        item(key = "app_streaks_grid_row") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = state.appStreaks,
                                    key = { "app_grid_${it.app.id}" }
                                ) { appItem ->
                                    AppStreakGridCard(
                                        appItem = appItem,
                                        onClick = { onNavigateToStreakDetail(appItem.app.id, StreakType.APP) },
                                        onLongClick = { longPressedTarget = LongPressStreakTarget.App(appItem) }
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = state.appStreaks,
                            key = { "app_${it.app.id}" }
                        ) { appItem ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                AppStreakCard(
                                    appItem = appItem,
                                    onClick = { onNavigateToStreakDetail(appItem.app.id, StreakType.APP) },
                                    onLongClick = { longPressedTarget = LongPressStreakTarget.App(appItem) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tap-outside scrim rendered BEHIND the ExpandableFabMenu
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isFabExpanded = false
                    }
            )
        }

        // Expandable FAB Menu rendered ABOVE the scrim so options are always clickable
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 96.dp)
        ) {
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
    }

    // Long-press Streak Settings Bottom Sheet (matches CommonStreakDetailScreen menu)
    longPressedTarget?.let { target ->
        val isNormal = target is LongPressStreakTarget.Normal
        val targetId = when (target) {
            is LongPressStreakTarget.Normal -> target.streak.id
            is LongPressStreakTarget.App -> target.item.app.id
        }
        val targetType = if (isNormal) StreakType.NORMAL else StreakType.APP
        val targetName = when (target) {
            is LongPressStreakTarget.Normal -> target.streak.title
            is LongPressStreakTarget.App -> target.item.app.appName
        }
        val isPaused = when (target) {
            is LongPressStreakTarget.Normal -> target.streak.isPaused
            is LongPressStreakTarget.App -> target.item.app.isPaused
        }
        val isArchived = when (target) {
            is LongPressStreakTarget.Normal -> target.streak.isArchived
            is LongPressStreakTarget.App -> target.item.app.isArchived
        }

        ModalBottomSheet(
            onDismissRequest = { longPressedTarget = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = targetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isNormal) "Habit Streak Settings" else "App Streak Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                StreakSettingSheetItem(
                    icon = Icons.Default.Edit,
                    label = "Edit Streak",
                    onClick = {
                        longPressedTarget = null
                        if (isNormal) {
                            onNavigateToEdit(targetId)
                        } else {
                            editingAppTarget = targetId to targetName
                        }
                    }
                )
                StreakSettingSheetItem(
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = if (isPaused) "Resume Streak" else "Pause Streak",
                    onClick = {
                        longPressedTarget = null
                        viewModel.togglePause(targetType, targetId)
                    }
                )
                StreakSettingSheetItem(
                    icon = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    label = if (isArchived) "Restore from Vault" else "Move to Archive",
                    onClick = {
                        longPressedTarget = null
                        viewModel.toggleArchive(targetType, targetId)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                StreakSettingSheetItem(
                    icon = Icons.Default.Delete,
                    label = "Delete Streak",
                    tint = AccentCoral,
                    onClick = {
                        longPressedTarget = null
                        viewModel.requestDelete(targetType, targetId)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Edit App Streak Dialog
    editingAppTarget?.let { (appId, currentName) ->
        var appNameText by remember(appId) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { editingAppTarget = null },
            title = { Text("Edit App Streak", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Update the display name of this tracked application:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = appNameText,
                        onValueChange = { appNameText = it },
                        label = { Text("App Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = appNameText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.updateAppName(appId, trimmed)
                            editingAppTarget = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAppTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (state.actionDialogState.showDeleteDialog) {
        val targetName = state.actionDialogState.deletionConfirmation?.name ?: "streak"
        val recordCount = state.actionDialogState.deletionConfirmation?.affectedRecordCount ?: 0
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = {
                Text(
                    text = "Delete Streak?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"$targetName\"? This action will remove $recordCount related record(s) and cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Cancel")
                }
            }
        )
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
private fun StreakSettingSheetItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
private fun gridCardStatusColors(status: CardCompletionStatus): Pair<Color, Color> {
    return when (status) {
        CardCompletionStatus.ALL_DONE -> SuccessGreen.copy(alpha = 0.18f) to SuccessGreen.copy(alpha = 0.55f)
        CardCompletionStatus.PARTIAL -> WarningAmber.copy(alpha = 0.18f) to WarningAmber.copy(alpha = 0.55f)
        CardCompletionStatus.NONE -> AccentCoral.copy(alpha = 0.14f) to AccentCoral.copy(alpha = 0.45f)
        CardCompletionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) to WarningAmber.copy(alpha = 0.4f)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalStreakGridCard(
    streak: Streak,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val todayStr = StreakCalculator.formatDate(AppClock.now())
    val status = when {
        streak.isPaused -> CardCompletionStatus.PAUSED
        streak.lastCompletedDate == todayStr -> CardCompletionStatus.ALL_DONE
        else -> CardCompletionStatus.NONE
    }
    val (containerColor, borderColor) = gridCardStatusColors(status)

    Card(
        modifier = Modifier
            .width(114.dp)
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            CornerStreakBadge(
                streakCount = streak.currentStreak,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = StreakIconHelper.getIcon(streak.icon),
                        contentDescription = streak.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = streak.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppStreakGridCard(
    appItem: HomeAppStreakItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val app = appItem.app
    val (containerColor, borderColor) = gridCardStatusColors(appItem.completionStatus)

    Card(
        modifier = Modifier
            .width(114.dp)
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            CornerAccountBadge(
                accountCount = appItem.accountCount,
                modifier = Modifier.align(Alignment.TopStart)
            )

            CornerStreakBadge(
                streakCount = appItem.displayStreak,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Box(modifier = Modifier.align(Alignment.Center)) {
                TrackedAppIcon(
                    packageName = app.packageName,
                    iconSize = 44.dp,
                    clipShape = RoundedCornerShape(12.dp)
                )
            }

            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CornerAccountBadge(
    accountCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "$accountCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CornerStreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = SecondaryTeal,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "$streakCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalStreakCard(
    streak: Streak,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val todayStr = StreakCalculator.formatDate(AppClock.now())
    val isDoneToday = streak.lastCompletedDate == todayStr
    val status = when {
        streak.isPaused -> CardCompletionStatus.PAUSED
        isDoneToday -> CardCompletionStatus.ALL_DONE
        else -> CardCompletionStatus.NONE
    }
    val (containerColor, borderColor) = when (status) {
        CardCompletionStatus.ALL_DONE -> SuccessGreen.copy(alpha = 0.12f) to SuccessGreen.copy(alpha = 0.4f)
        CardCompletionStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant to AccentCoral.copy(alpha = 0.35f)
        CardCompletionStatus.PARTIAL -> WarningAmber.copy(alpha = 0.14f) to WarningAmber.copy(alpha = 0.45f)
        CardCompletionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant to WarningAmber.copy(alpha = 0.35f)
    }

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
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppStreakCard(
    appItem: HomeAppStreakItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val app = appItem.app
    val (containerColor, borderColor) = when (appItem.completionStatus) {
        CardCompletionStatus.ALL_DONE -> SuccessGreen.copy(alpha = 0.12f) to SuccessGreen.copy(alpha = 0.4f)
        CardCompletionStatus.PARTIAL -> WarningAmber.copy(alpha = 0.14f) to WarningAmber.copy(alpha = 0.45f)
        CardCompletionStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant to AccentCoral.copy(alpha = 0.35f)
        CardCompletionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant to WarningAmber.copy(alpha = 0.35f)
    }

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
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
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
