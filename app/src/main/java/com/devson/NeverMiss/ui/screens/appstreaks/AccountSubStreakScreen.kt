package com.devson.NeverMiss.ui.screens.appstreaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakStatus
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.ui.components.AccountQuickSettingsSheet
import com.devson.NeverMiss.ui.components.UndoTodayStreakConfirmDialog
import com.devson.NeverMiss.ui.screens.detail.StreakStatisticsSection
import com.devson.NeverMiss.viewmodel.appstreaks.AccountDetailUiState
import com.devson.NeverMiss.viewmodel.appstreaks.AccountDetailViewModel
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSubStreakScreen(
    appId: Long,
    accountId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val account = state.account
    val app = state.app

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = account?.accountName ?: "Account Details",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app != null) {
                            Text(
                                text = "App: ${app.appName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.account == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Account not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    AccountDetailContent(
                        state = state,
                        onMarkDoneToday = viewModel::markDoneToday,
                        onUndoDoneToday = viewModel::undoDoneToday,
                        onMonthChange = viewModel::changeMonth
                    )
                }
            }
        }
    }

    if (showMenu && account != null) {
        AccountQuickSettingsSheet(
            accountName = account.accountName,
            isEnabled = account.isEnabled,
            onDismiss = { showMenu = false },
            onRename = {
                showMenu = false
                viewModel.setShowRenameDialog(true)
            },
            onToggleEnabled = {
                showMenu = false
                viewModel.toggleTracking()
            },
            onDelete = {
                showMenu = false
                showDeleteDialog = true
            }
        )
    }

    // Rename Dialog
    if (state.showRenameDialog && account != null) {
        var editName by remember { mutableStateOf(account.accountName) }

        AlertDialog(
            onDismissRequest = { viewModel.setShowRenameDialog(false) },
            title = { Text("Rename Account") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.renameAccount(editName.trim()) },
                    enabled = editName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowRenameDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently remove the account '${account.accountName}' and its logged completion history.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount(onDeleted = onNavigateBack)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountDetailContent(
    state: AccountDetailUiState,
    onMarkDoneToday: () -> Unit,
    onUndoDoneToday: () -> Unit,
    onMonthChange: (java.time.YearMonth) -> Unit
) {
    val account = state.account
    val stats = state.stats
    val completedDates = remember(state.completions) {
        state.completions.map { it.completionDate }.toSet()
    }
    var showUndoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (state.isCompletedToday) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = com.devson.NeverMiss.ui.theme.SecondaryTeal.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        com.devson.NeverMiss.ui.theme.SecondaryTeal.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = com.devson.NeverMiss.ui.theme.SecondaryTeal
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Completed Today!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "This account's streak is logged for today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showUndoDialog = true }) {
                            Text("Undo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onMarkDoneToday,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark Streak Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        if (account != null && stats != null) {
            item {
                val createdDateStr = remember(account.createdAt) {
                    StreakCalculator.formatDate(
                        Instant.ofEpochMilli(account.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )
                }
                val startDateStr = completedDates.minOrNull() ?: createdDateStr
                val accountDetail = SharedStreakDetail(
                    id = account.id,
                    type = StreakType.APP,
                    name = account.accountName,
                    description = state.app?.appName,
                    icon = null,
                    emoji = null,
                    color = null,
                    currentStreak = stats.currentStreak,
                    longestStreak = stats.longestStreak,
                    lastCompletedDate = account.lastCompletedDate,
                    isPaused = account.isPaused,
                    isArchived = account.isArchived,
                    status = if (state.isCompletedToday) StreakStatus.COMPLETED_TODAY else StreakStatus.ACTIVE,
                    creationDate = createdDateStr,
                    startDate = startDateStr,
                    totalCompletedDays = stats.totalCompletedDays,
                    updatedAt = account.updatedAt
                )
                StreakStatisticsSection(detail = accountDetail)
            }
        }

        // Calendar
        item {
            Text(
                text = "ACCOUNT COMPLETIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            CompletionCalendarView(
                completedDates = completedDates,
                currentMonth = state.selectedYearMonth,
                onMonthChange = onMonthChange
            )
        }

        // History
        item {
            Text(
                text = "LOGGED SESSIONS (${state.completions.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.completions.isEmpty()) {
            item {
                Text(
                    text = "No sessions logged yet for this account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.completions, key = { it.id }) { completion ->
                CompletionHistoryItem(completion = completion)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showUndoDialog && account != null) {
        UndoTodayStreakConfirmDialog(
            streakName = account.accountName,
            onDismiss = { showUndoDialog = false },
            onConfirm = {
                showUndoDialog = false
                onUndoDoneToday()
            }
        )
    }
}

@Composable
private fun AccountStatBox(
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
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletionHistoryItem(completion: AppStreakCompletion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = completion.completionDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Manual Confirmation",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

