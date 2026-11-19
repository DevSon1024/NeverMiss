package com.devson.NeverMiss.core.apptracking.engine

import android.app.usage.UsageEvents
import com.devson.NeverMiss.core.apptracking.model.AppForegroundEvent
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType

object UsageEventNormalizer {

    @Suppress("DEPRECATION")
    fun normalizeEventType(rawEventType: Int): NormalizedEventType {
        return when (rawEventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.MOVE_TO_FOREGROUND -> NormalizedEventType.FOREGROUND

            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> NormalizedEventType.BACKGROUND

            else -> NormalizedEventType.OTHER
        }
    }

    fun toNormalizedEvent(
        packageName: String,
        timestamp: Long,
        rawEventType: Int
    ): AppForegroundEvent {
        return AppForegroundEvent(
            packageName = packageName,
            timestamp = timestamp,
            eventType = normalizeEventType(rawEventType)
        )
    }
}

