package com.devson.NeverMiss.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val icon: String? = null,
    val emoji: String,
    val category: String,
    val color: Int,
    val creationDate: String,
    val startDate: String = creationDate,
    val isArchived: Boolean = false,
    val isPaused: Boolean = false,
    
    val repeatIntervalDays: Int = 1,
    val daysOfWeek: String = "",
    
    val reminderTime: String = "10:00",
    val reminderRepeatHours: Int = 0,
    val remindUntilDone: Boolean = false,
    val dangerReminderMinutes: Int = 0,
    val cutoffTime: String = "00:00",
    val gracePeriodUntil3AM: Boolean = false,
    
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "",
    
    val notificationSound: String? = null,
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val persistent: Boolean = false,
    
    val nextReminderTimeMillis: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
