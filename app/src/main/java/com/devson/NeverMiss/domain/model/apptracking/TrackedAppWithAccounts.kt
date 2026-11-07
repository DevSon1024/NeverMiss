package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class TrackedAppWithAccounts(
    val app: TrackedApp,
    val accounts: List<TrackedAccount> = emptyList()
)

