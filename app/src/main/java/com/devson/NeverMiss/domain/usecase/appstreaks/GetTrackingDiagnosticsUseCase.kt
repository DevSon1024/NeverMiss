package com.devson.NeverMiss.domain.usecase.appstreaks

import android.content.Context
import android.os.PowerManager
import androidx.work.WorkManager
import com.devson.NeverMiss.core.apptracking.permission.UsageAccessPermissionHelper
import com.devson.NeverMiss.core.apptracking.worker.AppUsageReconciliationWorker
import com.devson.NeverMiss.domain.model.apptracking.TrackingDiagnosticsInfo
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetTrackingDiagnosticsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(): TrackingDiagnosticsInfo {
        val hasPermission = UsageAccessPermissionHelper.hasUsageAccessPermission(context)
        val checkpoint = appStreakRepository.getCheckpoint()

        val checkpointFormatted = checkpoint?.lastProcessedTimestamp?.let { millis ->
            val instant = Instant.ofEpochMilli(millis)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } ?: "No checkpoint recorded yet"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        val workManagerStatus = try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(AppUsageReconciliationWorker.WORK_NAME)
                .get()
            workInfos.firstOrNull()?.state?.name ?: "ENQUEUED"
        } catch (e: Exception) {
            "SCHEDULED"
        }

        val enabledApps = appStreakRepository.getEnabledTrackedApps()
        val pendingConfirmations = appStreakRepository.getActivePendingConfirmationsFlow().firstOrNull() ?: emptyList()

        return TrackingDiagnosticsInfo(
            hasUsageAccessPermission = hasPermission,
            lastCheckpointTimestamp = checkpoint?.lastProcessedTimestamp,
            lastCheckpointFormatted = checkpointFormatted,
            isBatteryOptimizationIgnored = isBatteryIgnored,
            workManagerStatus = workManagerStatus,
            trackedAppsCount = enabledApps.size,
            pendingConfirmationsCount = pendingConfirmations.size
        )
    }
}
