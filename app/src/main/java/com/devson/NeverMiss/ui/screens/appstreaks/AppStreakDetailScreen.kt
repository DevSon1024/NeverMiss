package com.devson.NeverMiss.ui.screens.appstreaks

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.usecase.appstreaks.TrackedAccountWithStats
import com.devson.NeverMiss.viewmodel.appstreaks.AppStreakDetailUiState
import com.devson.NeverMiss.viewmodel.appstreaks.AppStreakDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStreakDetailScreen(
    appId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAccount: (Long, Long) -> Unit,
    viewModel: AppStreakDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val app = state.detail?.app

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = app?.appName ?: "App Streak Details",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (app?.isEnabled == true) "Disable Tracking" else "Enable Tracking") },
                            onClick = {
                                showMenu = false
                                viewModel.toggleAppTracking()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete App Tracking", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
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
                state.detail == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("App details not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    DetailContent(
                        state = state,
                        onMonthChange = viewModel::changeMonth,
                        onShowAddAccount = { viewModel.showAddAccountDialog(true) },
                        onEditAccount = { viewModel.setEditingAccount(it) },
                        onToggleAccountTracking = viewModel::toggleAccountTracking,
                        onDeleteAccount = viewModel::deleteAccount,
                        onAccountClick = { accountId -> onNavigateToAccount(appId, accountId) }
                    )
                }
            }
        }
    }

    // Add Account Dialog
    if (state.showAddAccountDialog) {
        AddOrEditAccountDialog(
            title = "Add Account Sub-Streak",
            initialName = "",
            confirmButtonLabel = "Add",
            showStreakInput = true,
            onDismiss = { viewModel.showAddAccountDialog(false) },
            onConfirm = { name, initialStreak -> viewModel.addAccount(name, initialStreak) }
        )
    }

    // Edit Account Dialog
    state.editingAccount?.let { account ->
        AddOrEditAccountDialog(
            title = "Rename Account",
            initialName = account.accountName,
            confirmButtonLabel = "Save",
            showStreakInput = false,
            onDismiss = { viewModel.setEditingAccount(null) },
            onConfirm = { name, _ -> viewModel.renameAccount(account.id, name) }
        )
    }

    // Delete App Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Tracked App?") },
            text = { Text("This will remove tracking for ${app?.appName} along with its completions and all account sub-streaks.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteAppTracking(onDeleted = onNavigateBack)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    state: AppStreakDetailUiState,
    onMonthChange: (java.time.YearMonth) -> Unit,
    onShowAddAccount: () -> Unit,
    onEditAccount: (TrackedAccount) -> Unit,
    onToggleAccountTracking: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onAccountClick: (Long) -> Unit
) {
    val detail = state.detail ?: return
    val completedDates = remember(detail.completions) {
        detail.completions.map { it.completionDate }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val isSingleAccount = detail.accountsWithStats.size == 1

        // App Stats Row (Only when exactly one account)
        if (isSingleAccount) {
            item {
                AppStatsHeader(stats = detail.stats)
            }
        }

        // Monthly Calendar (Only when exactly one account)
        if (isSingleAccount) {
            item {
                Text(
                    text = "COMPLETION CALENDAR",
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
        }

        // Accounts Sub-Streaks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACCOUNT SUB-STREAKS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onShowAddAccount) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Account")
                }
            }
        }

        if (detail.accountsWithStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Sub-Accounts Configured",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add named accounts (e.g., Personal, Developer, Alt) to track separate sub-streaks for this app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(detail.accountsWithStats, key = { it.account.id }) { item ->
                AccountSubStreakItem(
                    accountWithStats = item,
                    onClick = { onAccountClick(item.account.id) },
                    onEdit = { onEditAccount(item.account) },
                    onToggle = { onToggleAccountTracking(item.account.id) },
                    onDelete = { onDeleteAccount(item.account.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppStatsHeader(stats: com.devson.NeverMiss.domain.model.apptracking.AppStreakStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBox(
            title = "Current Streak",
            value = "${stats.currentStreak}d",
            icon = Icons.Default.Whatshot,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            title = "Longest Streak",
            value = "${stats.longestStreak}d",
            icon = Icons.Default.Whatshot,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            title = "Total Days",
            value = "${stats.totalCompletedDays}d",
            icon = Icons.Default.Person,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBox(
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
private fun AccountSubStreakItem(
    accountWithStats: TrackedAccountWithStats,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val account = accountWithStats.account
    val stats = accountWithStats.stats

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.devson.NeverMiss.core.util.AccountIcon(
                iconSize = 36.dp,
                clipShape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stats.currentStreak}d streak • Best: ${stats.longestStreak}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename Account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddOrEditAccountDialog(
    title: String,
    initialName: String,
    confirmButtonLabel: String,
    showStreakInput: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var initialStreakStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g., Personal, Developer, Alt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showStreakInput) {
                    OutlinedTextField(
                        value = initialStreakStr,
                        onValueChange = { initialStreakStr = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Starting Streak (Days, Optional)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            val count = initialStreakStr.toIntOrNull() ?: 0
                            if (count > 0) {
                                Text("Starts at $count days; advances to ${count + 1} upon today's usage.")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        val streak = initialStreakStr.toIntOrNull() ?: 0
                        onConfirm(name.trim(), streak)
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

