package com.devson.NeverMiss.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SharedStreakDetail(
    val id: Long,
    val type: StreakType,
    val name: String,
    val description: String?,
    val icon: String? = null,
    val emoji: String? = null,
    val color: Int? = null,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastCompletedDate: String,
    val isPaused: Boolean,
    val isArchived: Boolean,
    val status: StreakStatus,
    val creationDate: String,
    val startDate: String,
    val totalCompletedDays: Int,
    val updatedAt: Long
)
