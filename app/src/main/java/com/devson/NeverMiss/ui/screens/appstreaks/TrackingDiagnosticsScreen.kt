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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.apptracking.TrackingDiagnosticsInfo
import com.devson.NeverMiss.viewmodel.appstreaks.TrackingDiagnosticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrackingDiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tracking Diagnostics",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                    IconButton(onClick = viewModel::loadDiagnostics) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Diagnostics"
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
            if (state.isLoading && state.info == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                DiagnosticsList(
                    info = state.info,
                    isScanning = state.isScanning,
                    scanResultBanner = state.scanResultBanner,
                    onRunScan = viewModel::runManualScan,
                    onOpenUsageSettings = viewModel::openUsageAccessSettings,
                    onOpenBatterySettings = viewModel::openBatteryOptimizationSettings
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsList(
    info: TrackingDiagnosticsInfo?,
    isScanning: Boolean,
    scanResultBanner: String?,
    onRunScan: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (scanResultBanner != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = scanResultBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Permission Card
        item {
            val hasPerm = info?.hasUsageAccessPermission == true
            DiagnosticsCard(
                title = "Usage Access Permission",
                status = if (hasPerm) "Active (Granted)" else "Permission Missing",
                statusColor = if (hasPerm) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                icon = if (hasPerm) Icons.Default.CheckCircle else Icons.Default.Error,
                description = if (hasPerm) {
                    "Android UsageStatsManager is accessible. NeverMiss can read foreground application events."
                } else {
                    "Usage access is required to detect app usage. Tap below to grant permission in system settings."
                },
                actionButton = if (!hasPerm) {
                    {
                        Button(onClick = onOpenUsageSettings) {
                            Text("Grant Usage Access")
                        }
                    }
                } else null
            )
        }

        // 2. Checkpoint & Event Engine
        item {
            DiagnosticsCard(
                title = "Processing Checkpoint",
                status = "Last Processed",
                statusColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Schedule,
                description = "Timestamp: ${info?.lastCheckpointFormatted}\nDebounce Window: 2000ms\nTracked Apps Active: ${info?.trackedAppsCount ?: 0}"
            )
        }

        // 3. WorkManager Background Worker
        item {
            DiagnosticsCard(
                title = "Background WorkManager",
                status = info?.workManagerStatus ?: "SCHEDULED",
                statusColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Settings,
                description = "Periodic Interval: 15 minutes (Android system minimum)\nUnique Work: AppUsageReconciliationWorker\nPending Confirmations: ${info?.pendingConfirmationsCount ?: 0}"
            )
        }

        // 4. Battery Optimization Guidance
        item {
            val isIgnored = info?.isBatteryOptimizationIgnored == true
            DiagnosticsCard(
                title = "Battery Management",
                status = if (isIgnored) "Unrestricted" else "Standard (Optimized)",
                statusColor = if (isIgnored) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.BatteryChargingFull,
                description = "Aggressive OEM battery savers (e.g. Xiaomi MIUI, Huawei, Samsung Sleeping Apps) can delay periodic WorkManager tasks. For best background reliability, set NeverMiss to unrestricted.",
                actionButton = {
                    OutlinedButton(onClick = onOpenBatterySettings) {
                        Text("Review Battery Settings")
                    }
                }
            )
        }

        // 5. Test Manual Scan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manual Diagnostic Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trigger an on-demand reconciliation scan through the tracking engine to test detection of recently opened apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRunScan,
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running Scan...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Test Usage Scan Now")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DiagnosticsCard(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    description: String,
    actionButton: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(12.dp))
                actionButton()
            }
        }
    }
}

