package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class TrackingDiagnosticsInfo(
    val hasUsageAccessPermission: Boolean,
    val lastCheckpointTimestamp: Long?,
    val lastCheckpointFormatted: String,
    val isBatteryOptimizationIgnored: Boolean,
    val workManagerStatus: String,
    val trackedAppsCount: Int,
    val pendingConfirmationsCount: Int
)

