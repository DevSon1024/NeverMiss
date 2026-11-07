package com.devson.NeverMiss.core.apptracking.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppForegroundEvent(
    val packageName: String,
    val timestamp: Long,
    val eventType: NormalizedEventType
)

enum class NormalizedEventType {
    FOREGROUND,
    BACKGROUND,
    OTHER
}

