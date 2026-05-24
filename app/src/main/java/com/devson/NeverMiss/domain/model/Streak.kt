package com.devson.NeverMiss.domain.model

import com.devson.NeverMiss.data.local.db.StreakEntity

data class Streak(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val emoji: String,
    val category: StreakCategory,
    val color: Int,
    val creationDate: String, // yyyy-MM-dd
    val isArchived: Boolean = false,
    val isPaused: Boolean = false,
    
    // Repeat logic
    val repeatIntervalDays: Int = 1,
    val daysOfWeek: List<Int> = emptyList(), // 1=Mon, 7=Sun
    
    // Reminder settings
    val reminderTime: String = "10:00", // HH:mm
    val reminderRepeatHours: Int = 0,
    val remindUntilDone: Boolean = false,
    val dangerReminderMinutes: Int = 0,
    val cutoffTime: String = "00:00", // HH:mm
    val gracePeriodUntil3AM: Boolean = false,
    
    // Stats
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "", // yyyy-MM-dd
    
    // Notifications & Alarm settings
    val notificationSound: String? = null,
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val persistent: Boolean = false,
    
    val nextReminderTimeMillis: Long = 0L
) {
    fun toEntity(): StreakEntity {
        return StreakEntity(
            id = id,
            title = title,
            description = description,
            emoji = emoji,
            category = category.name,
            color = color,
            creationDate = creationDate,
            isArchived = isArchived,
            isPaused = isPaused,
            repeatIntervalDays = repeatIntervalDays,
            daysOfWeek = daysOfWeek.joinToString(","),
            reminderTime = reminderTime,
            reminderRepeatHours = reminderRepeatHours,
            remindUntilDone = remindUntilDone,
            dangerReminderMinutes = dangerReminderMinutes,
            cutoffTime = cutoffTime,
            gracePeriodUntil3AM = gracePeriodUntil3AM,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastCompletedDate = lastCompletedDate,
            notificationSound = notificationSound,
            vibrate = vibrate,
            silent = silent,
            persistent = persistent,
            nextReminderTimeMillis = nextReminderTimeMillis
        )
    }

    companion object {
        fun fromEntity(entity: StreakEntity): Streak {
            val daysList = if (entity.daysOfWeek.isNotEmpty()) {
                entity.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
            } else emptyList()
            
            return Streak(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                emoji = entity.emoji,
                category = StreakCategory.fromName(entity.category),
                color = entity.color,
                creationDate = entity.creationDate,
                isArchived = entity.isArchived,
                isPaused = entity.isPaused,
                repeatIntervalDays = entity.repeatIntervalDays,
                daysOfWeek = daysList,
                reminderTime = entity.reminderTime,
                reminderRepeatHours = entity.reminderRepeatHours,
                remindUntilDone = entity.remindUntilDone,
                dangerReminderMinutes = entity.dangerReminderMinutes,
                cutoffTime = entity.cutoffTime,
                gracePeriodUntil3AM = entity.gracePeriodUntil3AM,
                currentStreak = entity.currentStreak,
                longestStreak = entity.longestStreak,
                lastCompletedDate = entity.lastCompletedDate,
                notificationSound = entity.notificationSound,
                vibrate = entity.vibrate,
                silent = entity.silent,
                persistent = entity.persistent,
                nextReminderTimeMillis = entity.nextReminderTimeMillis
            )
        }
    }
}
