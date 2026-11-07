package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class TrackedApp(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val trackingCreatedAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = ""
)

