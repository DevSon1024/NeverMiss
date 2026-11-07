package com.devson.NeverMiss.core.apptracking.engine

import android.app.usage.UsageEvents
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageEventNormalizerTest {

    @Test
    @Suppress("DEPRECATION")
    fun testNormalizeEventType_foregroundTypes() {
        assertEquals(
            NormalizedEventType.FOREGROUND,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.ACTIVITY_RESUMED)
        )
        assertEquals(
            NormalizedEventType.FOREGROUND,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.MOVE_TO_FOREGROUND)
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun testNormalizeEventType_backgroundTypes() {
        assertEquals(
            NormalizedEventType.BACKGROUND,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.ACTIVITY_PAUSED)
        )
        assertEquals(
            NormalizedEventType.BACKGROUND,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.MOVE_TO_BACKGROUND)
        )
    }

    @Test
    fun testNormalizeEventType_otherTypes() {
        assertEquals(
            NormalizedEventType.OTHER,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.USER_INTERACTION)
        )
        assertEquals(
            NormalizedEventType.OTHER,
            UsageEventNormalizer.normalizeEventType(UsageEvents.Event.STANDBY_BUCKET_CHANGED)
        )
        assertEquals(
            NormalizedEventType.OTHER,
            UsageEventNormalizer.normalizeEventType(9999)
        )
    }

    @Test
    fun testToNormalizedEvent_createsForegroundEventProperly() {
        val event = UsageEventNormalizer.toNormalizedEvent(
            packageName = "com.reddit.frontpage",
            timestamp = 1700000000000L,
            rawEventType = UsageEvents.Event.ACTIVITY_RESUMED
        )

        assertEquals("com.reddit.frontpage", event.packageName)
        assertEquals(1700000000000L, event.timestamp)
        assertEquals(NormalizedEventType.FOREGROUND, event.eventType)
    }
}

