package com.devson.NeverMiss.core.apptracking.engine

import android.content.Context
import com.devson.NeverMiss.core.apptracking.model.AppForegroundEvent
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
    val eventCount: Int,
    val totalForegroundDurationMillis: Long = 0L,
    val maxSingleSessionDurationMillis: Long = 0L,
    val hasVerified15SecondSession: Boolean = maxSingleSessionDurationMillis >= 15_000L
)

data class UsageDetectionReport(
    val queryStartTime: Long,
    val queryEndTime: Long,
    val totalEventsExamined: Int,
    val detectedAppUsages: List<TrackedAppUsageDetection>,
    val updatedCheckpoint: Long,
    val isPermissionGranted: Boolean
)

data class AppSession(
    val packageName: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMillis: Long = endTimestamp - startTimestamp
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
        const val MIN_VERIFIED_DURATION_MILLIS = 15_000L // 15 seconds
        const val MAX_PLAUSIBLE_SESSION_MILLIS = 6 * 60 * 60 * 1000L // 6 hours safety cap
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

        val verifiedSessions = reconstructForegroundSessions(events)

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

            val lastTimestamp = timestamps.lastOrNull()
            if (lastTimestamp == null || (event.timestamp - lastTimestamp) >= DEBOUNCE_WINDOW_MILLIS) {
                timestamps.add(event.timestamp)
            }
        }

        val sessionsByAppDate = mutableMapOf<String, MutableList<AppSession>>()
        for (session in verifiedSessions) {
            val app = enabledApps[session.packageName] ?: continue
            val date = StreakCalculator.formatDate(
                Instant.ofEpochMilli(session.startTimestamp).atZone(zoneId).toLocalDate()
            )
            val key = "${app.id}_$date"
            sessionsByAppDate.getOrPut(key) { mutableListOf() }.add(session)
        }

        val allKeys = (detectionsMap.keys + sessionsByAppDate.keys).toSet()

        val detections = mutableListOf<TrackedAppUsageDetection>()
        for (key in allKeys) {
            val parts = key.split("_")
            val appId = parts[0].toLongOrNull() ?: continue
            val date = parts.getOrNull(1) ?: continue
            val app = enabledApps.values.find { it.id == appId } ?: continue

            val timestamps = detectionsMap[key] ?: emptyList()
            val sessions = sessionsByAppDate[key] ?: emptyList()

            val firstTimestamp = when {
                timestamps.isNotEmpty() && sessions.isNotEmpty() -> minOf(timestamps.first(), sessions.minOf { it.startTimestamp })
                timestamps.isNotEmpty() -> timestamps.first()
                sessions.isNotEmpty() -> sessions.minOf { it.startTimestamp }
                else -> startTime
            }

            val lastTimestamp = when {
                timestamps.isNotEmpty() && sessions.isNotEmpty() -> maxOf(timestamps.last(), sessions.maxOf { it.endTimestamp })
                timestamps.isNotEmpty() -> timestamps.last()
                sessions.isNotEmpty() -> sessions.maxOf { it.endTimestamp }
                else -> endTime
            }

            val totalDuration = sessions.sumOf { it.durationMillis }
            val maxSingleSessionDuration = if (sessions.isNotEmpty()) sessions.maxOf { it.durationMillis } else 0L

            detections.add(
                TrackedAppUsageDetection(
                    trackedApp = app,
                    firstDetectedTimestamp = firstTimestamp,
                    lastDetectedTimestamp = lastTimestamp,
                    sessionDate = date,
                    eventCount = maxOf(timestamps.size, sessions.size),
                    totalForegroundDurationMillis = totalDuration,
                    maxSingleSessionDurationMillis = maxSingleSessionDuration,
                    hasVerified15SecondSession = maxSingleSessionDuration >= MIN_VERIFIED_DURATION_MILLIS
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

    fun reconstructForegroundSessions(events: List<AppForegroundEvent>): List<AppSession> {
        val sorted = events.sortedBy { it.timestamp }
        val sessions = mutableListOf<AppSession>()

        var currentPackage: String? = null
        var currentStart: Long = 0L

        for (event in sorted) {
            when (event.eventType) {
                NormalizedEventType.FOREGROUND -> {
                    if (currentPackage != null) {
                        if (currentPackage == event.packageName) {
                            // Duplicate FOREGROUND event for the same app: ignore, retain initial start
                        } else {
                            // App switch: close previous app's session
                            val duration = event.timestamp - currentStart
                            if (duration > 0 && duration <= MAX_PLAUSIBLE_SESSION_MILLIS) {
                                sessions.add(
                                    AppSession(
                                        packageName = currentPackage,
                                        startTimestamp = currentStart,
                                        endTimestamp = event.timestamp
                                    )
                                )
                            }
                            currentPackage = event.packageName
                            currentStart = event.timestamp
                        }
                    } else {
                        currentPackage = event.packageName
                        currentStart = event.timestamp
                    }
                }
                NormalizedEventType.BACKGROUND -> {
                    if (currentPackage != null && currentPackage == event.packageName) {
                        val duration = event.timestamp - currentStart
                        if (duration > 0 && duration <= MAX_PLAUSIBLE_SESSION_MILLIS) {
                            sessions.add(
                                AppSession(
                                    packageName = currentPackage,
                                    startTimestamp = currentStart,
                                    endTimestamp = event.timestamp
                                )
                            )
                        }
                        currentPackage = null
                        currentStart = 0L
                    }
                }
                NormalizedEventType.SCREEN_OFF -> {
                    if (currentPackage != null) {
                        val duration = event.timestamp - currentStart
                        if (duration > 0 && duration <= MAX_PLAUSIBLE_SESSION_MILLIS) {
                            sessions.add(
                                AppSession(
                                    packageName = currentPackage,
                                    startTimestamp = currentStart,
                                    endTimestamp = event.timestamp
                                )
                            )
                        }
                        currentPackage = null
                        currentStart = 0L
                    }
                }
                NormalizedEventType.OTHER -> {
                    // No action
                }
            }
        }

        return sessions
    }
}
