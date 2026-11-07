package com.devson.NeverMiss.core.apptracking.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.devson.NeverMiss.core.apptracking.model.AppForegroundEvent
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun queryUsageEvents(startTime: Long, endTime: Long): List<AppForegroundEvent> = withContext(Dispatchers.IO) {
        if (startTime >= endTime) return@withContext emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext emptyList()

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
                ?: return@withContext emptyList()

            val event = UsageEvents.Event()
            val result = mutableListOf<AppForegroundEvent>()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val packageName = event.packageName ?: continue
                if (packageName.isBlank()) continue

                val normalized = UsageEventNormalizer.toNormalizedEvent(
                    packageName = packageName,
                    timestamp = event.timeStamp,
                    rawEventType = event.eventType
                )

                if (normalized.eventType != NormalizedEventType.OTHER) {
                    result.add(normalized)
                }
            }

            result.sortedBy { it.timestamp }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

