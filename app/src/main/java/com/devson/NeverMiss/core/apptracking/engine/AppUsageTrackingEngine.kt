package com.devson.NeverMiss.core.apptracking.engine

import android.content.Context
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType
import com.devson.NeverMiss.core.apptracking.permission.UsageAccessPermissionHelper
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class TrackedAppUsageDetection(
    val trackedApp: TrackedApp,
    val firstDetectedTimestamp: Long,
    val lastDetectedTimestamp: Long,
    val sessionDate: String, // yyyy-MM-dd
    val eventCount: Int
)

data class UsageDetectionReport(
    val queryStartTime: Long,
    val queryEndTime: Long,
    val totalEventsExamined: Int,
    val detectedAppUsages: List<TrackedAppUsageDetection>,
    val updatedCheckpoint: Long,
    val isPermissionGranted: Boolean
)

@Singleton
class AppUsageTrackingEngine @Inject constructor(
    private val usageStatsManagerHelper: UsageStatsManagerHelper,
    private val appStreakRepository: AppStreakRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_LOOKBACK_MILLIS = 24 * 60 * 60 * 1000L // 24 hours fallback
        const val DEBOUNCE_WINDOW_MILLIS = 2000L // 2 seconds between duplicate foreground transitions
    }

    suspend fun scanAndDetectUsage(
        endTime: Long = System.currentTimeMillis()
    ): UsageDetectionReport = withContext(Dispatchers.IO) {
        val hasPermission = UsageAccessPermissionHelper.hasUsageAccessPermission(context)
        if (!hasPermission) {
            return@withContext UsageDetectionReport(
                queryStartTime = endTime,
                queryEndTime = endTime,
                totalEventsExamined = 0,
                detectedAppUsages = emptyList(),
                updatedCheckpoint = endTime,
                isPermissionGranted = false
            )
        }

        val enabledApps = appStreakRepository.getEnabledTrackedApps().associateBy { it.packageName }
        val checkpoint = appStreakRepository.getCheckpoint(UsageCheckpoint.DEFAULT_SOURCE)
        var startTime = checkpoint?.lastProcessedTimestamp ?: (endTime - DEFAULT_LOOKBACK_MILLIS)

        if (startTime >= endTime) {
            // Checkpoint is ahead of current time, adjust with safety window
            startTime = endTime - 60_000L
        }

        if (enabledApps.isEmpty()) {
            appStreakRepository.saveCheckpoint(UsageCheckpoint.DEFAULT_SOURCE, endTime)
            return@withContext UsageDetectionReport(
                queryStartTime = startTime,
                queryEndTime = endTime,
                totalEventsExamined = 0,
                detectedAppUsages = emptyList(),
                updatedCheckpoint = endTime,
                isPermissionGranted = true
            )
        }

        val events = usageStatsManagerHelper.queryUsageEvents(startTime, endTime)
        val zoneId = ZoneId.systemDefault()

        // Filter for foreground events matching enabled tracked apps
        val matchingEvents = events.filter {
            it.eventType == NormalizedEventType.FOREGROUND && enabledApps.containsKey(it.packageName)
        }

        val detectionsMap = mutableMapOf<String, MutableList<Long>>() // key: "appId_date" -> timestamps

        for (event in matchingEvents) {
            val app = enabledApps[event.packageName] ?: continue
            val date = StreakCalculator.formatDate(
                Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
            )
            val key = "${app.id}_$date"
            val timestamps = detectionsMap.getOrPut(key) { mutableListOf() }

            // Debounce rapid duplicate foreground events
            val lastTimestamp = timestamps.lastOrNull()
            if (lastTimestamp == null || (event.timestamp - lastTimestamp) >= DEBOUNCE_WINDOW_MILLIS) {
                timestamps.add(event.timestamp)
            }
        }

        val detections = mutableListOf<TrackedAppUsageDetection>()
        for ((key, timestamps) in detectionsMap) {
            val parts = key.split("_")
            val appId = parts[0].toLongOrNull() ?: continue
            val date = parts.getOrNull(1) ?: continue
            val app = enabledApps.values.find { it.id == appId } ?: continue

            detections.add(
                TrackedAppUsageDetection(
                    trackedApp = app,
                    firstDetectedTimestamp = timestamps.first(),
                    lastDetectedTimestamp = timestamps.last(),
                    sessionDate = date,
                    eventCount = timestamps.size
                )
            )
        }

        val newCheckpoint = if (events.isNotEmpty()) {
            events.maxOf { it.timestamp } + 1
        } else {
            endTime
        }

        appStreakRepository.saveCheckpoint(UsageCheckpoint.DEFAULT_SOURCE, newCheckpoint)

        UsageDetectionReport(
            queryStartTime = startTime,
            queryEndTime = endTime,
            totalEventsExamined = events.size,
            detectedAppUsages = detections,
            updatedCheckpoint = newCheckpoint,
            isPermissionGranted = true
        )
    }
}

