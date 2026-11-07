package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class AppStreakCompletion(
    val id: Long = 0L,
    val trackedAppId: Long,
    val accountId: Long? = null,
    val completionDate: String, // yyyy-MM-dd
    val detectedAt: Long = System.currentTimeMillis(),
    val completionMethod: CompletionMethod = CompletionMethod.AUTOMATIC_USAGE,
    val confirmationStatus: ConfirmationStatus = ConfirmationStatus.CONFIRMED
)

enum class CompletionMethod {
    AUTOMATIC_USAGE,
    MANUAL_CONFIRMATION,
    RECONCILIATION
}

enum class ConfirmationStatus {
    CONFIRMED,
    PENDING_CONFIRMATION,
    AUTO_CREDITED
}

