package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class UsageCheckpoint(
    val id: Long = 0L,
    val sourceIdentifier: String = DEFAULT_SOURCE,
    val lastProcessedTimestamp: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_SOURCE = "DEFAULT_USAGE_TRACKER"
    }
}

