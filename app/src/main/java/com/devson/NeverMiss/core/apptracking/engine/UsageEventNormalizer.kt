package com.devson.NeverMiss.core.apptracking.engine

import android.app.usage.UsageEvents
import com.devson.NeverMiss.core.apptracking.model.AppForegroundEvent
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType

object UsageEventNormalizer {

    const val EVENT_SCREEN_INTERACTIVE = 15
    const val EVENT_SCREEN_NON_INTERACTIVE = 16
    const val EVENT_KEYGUARD_SHOWN = 17
    const val EVENT_KEYGUARD_HIDDEN = 18
    const val EVENT_DEVICE_SHUTDOWN = 26
    const val EVENT_DEVICE_STARTUP = 27

    @Suppress("DEPRECATION")
    fun normalizeEventType(rawEventType: Int): NormalizedEventType {
        return when (rawEventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.MOVE_TO_FOREGROUND -> NormalizedEventType.FOREGROUND

            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> NormalizedEventType.BACKGROUND

            EVENT_SCREEN_NON_INTERACTIVE,
            EVENT_KEYGUARD_SHOWN,
            EVENT_DEVICE_SHUTDOWN -> NormalizedEventType.SCREEN_OFF

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
