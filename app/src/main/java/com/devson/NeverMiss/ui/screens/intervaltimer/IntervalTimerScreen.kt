package com.devson.NeverMiss.ui.screens.intervaltimer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.viewmodel.intervaltimer.IntervalTimerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalTimerScreen(
    onNavigateBack: () -> Unit,
    viewModel: IntervalTimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var timerToEdit by remember { mutableStateOf<IntervalTimer?>(null) }
    var timerToDelete by remember { mutableStateOf<IntervalTimer?>(null) }

    // Live countdown clock ticker: ticks every 1 second when any timer is RUNNING
    var currentNowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val hasRunningTimers = remember(uiState.timers) {
        uiState.timers.any { it.status == IntervalTimerStatus.RUNNING }
    }

    LaunchedEffect(hasRunningTimers) {
        while (hasRunningTimers) {
            delay(1000L)
            currentNowMillis = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Interval Timers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Focus countdowns & recurring alerts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Timer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (if notifications or exact alarms are denied)
            val permissions = uiState.permissionsSnapshot
            val context = androidx.compose.ui.platform.LocalContext.current
            if (permissions != null &&
                (permissions.notificationStatus == com.devson.NeverMiss.core.reminder.interval.PermissionStatus.DENIED ||
                 permissions.exactAlarmStatus == com.devson.NeverMiss.core.reminder.interval.PermissionStatus.DENIED)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Alert",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (permissions.notificationStatus == com.devson.NeverMiss.core.reminder.interval.PermissionStatus.DENIED) {
                                    "Notifications are disabled. Timers will run, but visible alerts will be suppressed."
                                } else {
                                    "Exact alarms are disabled. Triggers may be slightly delayed by system power saving."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val intent = if (permissions.notificationStatus == com.devson.NeverMiss.core.reminder.interval.PermissionStatus.DENIED) {
                                    viewModel.permissionManager.createNotificationSettingsIntent()
                                } else {
                                    viewModel.permissionManager.createExactAlarmSettingsIntent()
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                }
                            }
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }

            // Filter chips
            FilterChipRow(
                selectedStatus = uiState.filterStatus,
                onSelectStatus = { viewModel.setFilterStatus(it) }
            )

            // Content Area
            if (uiState.isLoading && uiState.timers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.timers.isEmpty()) {
                IntervalTimerEmptyState(onCreateClick = { showCreateDialog = true })
            } else {
                // Sorting: RUNNING first, PAUSED second, then IDLE / STOPPED / COMPLETED
                val sortedTimers = remember(uiState.timers) {
                    uiState.timers.sortedWith(
                        compareBy<IntervalTimer> {
                            when (it.status) {
                                IntervalTimerStatus.RUNNING -> 0
                                IntervalTimerStatus.PAUSED -> 1
                                IntervalTimerStatus.IDLE -> 2
                                IntervalTimerStatus.STOPPED -> 3
                                IntervalTimerStatus.COMPLETED -> 4
                            }
                        }.thenByDescending { it.updatedAtMillis }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = sortedTimers,
                        key = { it.id }
                    ) { timer ->
                        IntervalTimerCard(
                            timer = timer,
                            currentNowMillis = currentNowMillis,
                            onStart = { viewModel.startTimer(timer.id) },
                            onPause = { viewModel.pauseTimer(timer.id) },
                            onResume = { viewModel.resumeTimer(timer.id) },
                            onStop = { viewModel.stopTimer(timer.id) },
                            onRestart = { viewModel.startTimer(timer.id) },
                            onEdit = { timerToEdit = timer },
                            onDelete = { timerToDelete = timer }
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Dialog
    if (showCreateDialog || timerToEdit != null) {
        val editingTimer = timerToEdit
        CreateEditIntervalTimerDialog(
            initialTimer = editingTimer,
            onDismiss = {
                showCreateDialog = false
                timerToEdit = null
            },
            onSave = { title, mode, durationMillis, intervalMillis, repeatLimit ->
                if (editingTimer == null) {
                    viewModel.createTimer(
                        title = title,
                        mode = mode,
                        durationMillis = durationMillis,
                        intervalMillis = intervalMillis,
                        repeatLimit = repeatLimit
                    )
                } else {
                    val updated = editingTimer.copy(
                        title = title,
                        mode = mode,
                        durationMillis = if (mode == IntervalTimerMode.COUNTDOWN) durationMillis else 0L,
                        intervalMillis = if (mode == IntervalTimerMode.INTERVAL) intervalMillis else 0L,
                        repeatLimit = if (mode == IntervalTimerMode.INTERVAL) repeatLimit else null
                    )
                    viewModel.updateTimer(updated)
                }
                showCreateDialog = false
                timerToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    timerToDelete?.let { timer ->
        AlertDialog(
            onDismissRequest = { timerToDelete = null },
            title = { Text("Delete Timer") },
            text = { Text("Are you sure you want to delete \"${timer.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTimer(timer.id)
                        timerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { timerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    selectedStatus: IntervalTimerStatus?,
    onSelectStatus: (IntervalTimerStatus?) -> Unit
) {
    val statuses = listOf(
        null to "All",
        IntervalTimerStatus.RUNNING to "Running",
        IntervalTimerStatus.PAUSED to "Paused",
        IntervalTimerStatus.IDLE to "Idle",
        IntervalTimerStatus.STOPPED to "Stopped",
        IntervalTimerStatus.COMPLETED to "Completed"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { (status, label) ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onSelectStatus(status) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
private fun IntervalTimerEmptyState(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Text(
                text = "No Timers Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Set up focused countdown sessions or recurring reminders for hydration, breaks, and routine intervals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCreateClick,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Timer")
            }
        }
    }
}
