package com.devson.NeverMiss.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,
    val color: Int, // Hex ARGB or Color Integer
    val creationDate: String, // yyyy-MM-dd
    val isArchived: Boolean = false,
    val isPaused: Boolean = false,
    
    // Repeat logic
    val repeatIntervalDays: Int = 1, // e.g. every 1 day, 2 days
    val daysOfWeek: String = "", // comma-separated: "1,2,3,4,5,6,7" (1=Monday, 7=Sunday)
    
    // Reminder engine
    val reminderTime: String = "10:00", // HH:mm
    val reminderRepeatHours: Int = 0, // e.g. repeat every 2 hours if not marked done
    val remindUntilDone: Boolean = false,
    val dangerReminderMinutes: Int = 0, // alert X minutes before cutoff time
    val cutoffTime: String = "00:00", // HH:mm (usually 00:00 or 03:00)
    val gracePeriodUntil3AM: Boolean = false, // cutoff shifts to 03:00 next day
    
    // Streak stats
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "", // yyyy-MM-dd
    
    // Alarm settings
    val notificationSound: String? = null,
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val persistent: Boolean = false,
    
    // Cache for ordering and easy presentation
    val nextReminderTimeMillis: Long = 0L
)
