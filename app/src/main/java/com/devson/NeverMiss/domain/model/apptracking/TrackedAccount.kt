package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class TrackedAccount(
    val id: Long = 0L,
    val trackedAppId: Long,
    val accountName: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "",
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
