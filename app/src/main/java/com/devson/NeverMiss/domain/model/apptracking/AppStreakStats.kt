package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable

@Immutable
data class AppStreakStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val isCompletedToday: Boolean
)

