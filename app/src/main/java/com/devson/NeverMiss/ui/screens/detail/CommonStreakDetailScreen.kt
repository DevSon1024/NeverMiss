package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.ui.theme.AccentCoral
import com.devson.NeverMiss.viewmodel.detail.CommonStreakDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonStreakDetailScreen(
    type: StreakType,
    id: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAccountDetail: (Long, Long) -> Unit = { _, _ -> },
    viewModel: CommonStreakDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showEditAppDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(type, id) {
        viewModel.load(type, id)
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onNavigateBack()
        }
    }

    val detail = state.detail

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.name ?: "Streak Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    detail?.let {
                        if (it.type == StreakType.APP) {
                            IconButton(onClick = { showAddAccountDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Account",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        StreakManagementMenu(
                            detail = it,
                            onTogglePause = viewModel::togglePause,
                            onToggleArchive = viewModel::toggleArchive,
                            onEdit = {
                                if (detail.type == StreakType.NORMAL) {
                                    onNavigateToEdit(detail.id)
                                } else {
                                    showEditAppDialog = true
                                }
                            },
                            onDelete = viewModel::requestDelete
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                detail == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Streak not found or already deleted",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Error Notification
                        AnimatedVisibility(
                            visible = state.actionError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            state.actionError?.let { msg ->
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

                        // HEADER
                        StreakDetailHeader(
                            detail = detail,
                            isCompletedToday = state.isCompletedToday
                        )

                        // STATISTICS
                        // When an App Streak has MORE THAN ONE account or NO accounts:
                        // Hide all three top statistics cards completely.
                        // When exactly ONE account or NORMAL streak: show statistics.
                        val showStatistics = detail.type == StreakType.NORMAL ||
                            (detail.type == StreakType.APP && state.accounts.size == 1)
                        if (showStatistics) {
                            StreakStatisticsSection(
                                detail = detail
                            )
                        }

                        // TYPE-SPECIFIC CONTENT
                        if (detail.type == StreakType.NORMAL) {
                            NormalStreakDetailContent(
                                detail = detail,
                                completedDates = state.completedDates,
                                missedDates = state.missedDates,
                                currentMonth = state.currentMonth,
                                isCompletedToday = state.isCompletedToday,
                                onCompleteToday = viewModel::completeToday,
                                onUndoToday = viewModel::undoToday,
                                onMonthChange = viewModel::changeMonth
                            )
                        } else {
                            AppStreakDetailContent(
                                detail = detail,
                                completedDates = state.completedDates,
                                currentMonth = state.currentMonth,
                                accounts = state.accounts,
                                reminderTime = state.appReminderTime,
                                onUpdateReminderTime = viewModel::updateAppReminderTime,
                                onMonthChange = viewModel::changeMonth,
                                onAccountClick = { accountId ->
                                    onNavigateToAccountDetail(detail.id, accountId)
                                },
                                onMarkAccountDone = viewModel::markAccountDoneToday,
                                onUndoAccountDone = viewModel::undoAccountDoneToday,
                                onRenameAccount = viewModel::renameAccount,
                                onToggleAccountEnabled = viewModel::toggleAccountEnabled,
                                onDeleteAccount = viewModel::deleteAccount,
                                onAddAccountClick = { showAddAccountDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        val targetName = state.deletionConfirmation?.name ?: detail?.name ?: "streak"
        val recordCount = state.deletionConfirmation?.affectedRecordCount ?: 0
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

    // Edit App Streak Dialog
    if (showEditAppDialog && detail?.type == StreakType.APP) {
        var appNameText by remember { mutableStateOf(detail.name) }
        AlertDialog(
            onDismissRequest = { showEditAppDialog = false },
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
                            viewModel.updateAppName(trimmed)
                            showEditAppDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Tracked Account Dialog
    if (showAddAccountDialog) {
        var accountNameText by remember { mutableStateOf("") }
        var initialStreakText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Tracked Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = accountNameText,
                        onValueChange = { accountNameText = it },
                        label = { Text("Account Name (e.g. Personal, Work)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialStreakText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) initialStreakText = it },
                        label = { Text("Initial Streak (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = accountNameText.trim()
                        if (trimmed.isNotEmpty()) {
                            val initial = initialStreakText.toIntOrNull() ?: 0
                            viewModel.addAccount(trimmed, initial)
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
