package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class PendingConfirmationWithDetails(
    val confirmation: PendingConfirmation,
    val app: TrackedApp,
    val availableAccounts: List<TrackedAccount>
)

