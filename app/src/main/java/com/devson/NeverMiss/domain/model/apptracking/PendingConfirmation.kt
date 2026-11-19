package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class PendingConfirmation(
    val id: Long = 0L,
    val trackedAppId: Long,
    val sessionTimestamp: Long,
    val sessionDate: String, // yyyy-MM-dd
    val status: PendingConfirmationStatus = PendingConfirmationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_EXPIRY_MILLIS
) {
    companion object {
        const val DEFAULT_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
    }
}

enum class PendingConfirmationStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    DISMISSED
}

