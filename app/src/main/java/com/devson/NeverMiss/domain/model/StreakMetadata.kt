package com.devson.NeverMiss.domain.model

interface StreakMetadata {
    val id: Long
    val type: StreakType
    val name: String
    val description: String?
    val currentStreak: Int
    val longestStreak: Int
    val lastCompletedDate: String
    val isPaused: Boolean
    val isArchived: Boolean
    val status: StreakStatus
    val updatedAt: Long
}
