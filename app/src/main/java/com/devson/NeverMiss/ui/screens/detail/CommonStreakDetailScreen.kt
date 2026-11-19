package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                        StreakManagementMenu(
                            detail = it,
                            onTogglePause = viewModel::togglePause,
                            onToggleArchive = viewModel::toggleArchive,
                            onEdit = { onNavigateToEdit(detail.id) },
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
                        StreakStatisticsSection(
                            detail = detail
                        )

                        // TYPE-SPECIFIC CONTENT
                        if (detail.type == StreakType.NORMAL) {
                            NormalStreakDetailContent(
                                detail = detail,
                                completedDates = state.completedDates,
                                missedDates = state.missedDates,
                                currentMonth = state.currentMonth,
                                isCompletedToday = state.isCompletedToday,
                                onCompleteToday = viewModel::completeToday,
                                onMonthChange = viewModel::changeMonth
                            )
                        } else {
                            AppStreakDetailContent(
                                detail = detail,
                                completedDates = state.completedDates,
                                currentMonth = state.currentMonth,
                                accounts = state.accounts,
                                onMonthChange = viewModel::changeMonth,
                                onAccountClick = { accountId ->
                                    onNavigateToAccountDetail(detail.id, accountId)
                                }
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
                    text = "Are you sure you want to permanently delete \"$targetName\"? This will remove all associated completion records ($recordCount recorded history items). This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::executeDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
