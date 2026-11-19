package com.devson.NeverMiss.domain.model

import androidx.compose.runtime.Immutable
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.data.local.db.StreakEntity

@Immutable
data class Streak(
    override val id: Long = 0L,
    val title: String,
    override val description: String = "",
    val icon: String? = null,
    val emoji: String = "✨",
    val category: StreakCategory,
    val color: Int,
    val creationDate: String, // yyyy-MM-dd
    val startDate: String = creationDate, // yyyy-MM-dd
    override val isArchived: Boolean = false,
    override val isPaused: Boolean = false,
    
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
    override val currentStreak: Int = 0,
    override val longestStreak: Int = 0,
    override val lastCompletedDate: String = "", // yyyy-MM-dd
    
    // Notifications & Alarm settings
    val notificationSound: String? = null,
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val persistent: Boolean = false,
    
    val nextReminderTimeMillis: Long = 0L,
    override val updatedAt: Long = System.currentTimeMillis()
) : StreakMetadata {

    override val type: StreakType get() = StreakType.NORMAL
    override val name: String get() = title
    override val status: StreakStatus
        get() = when {
            isArchived -> StreakStatus.ARCHIVED
            isPaused -> StreakStatus.PAUSED
            lastCompletedDate == StreakCalculator.formatDate(AppClock.now()) -> StreakStatus.COMPLETED_TODAY
            else -> StreakStatus.ACTIVE
        }

    fun toEntity(): StreakEntity {
        return StreakEntity(
            id = id,
            title = title,
            description = description,
            icon = icon,
            emoji = emoji,
            category = category.name,
            color = color,
            creationDate = creationDate,
            startDate = startDate,
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
            nextReminderTimeMillis = nextReminderTimeMillis,
            updatedAt = updatedAt
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
                icon = entity.icon,
                emoji = entity.emoji,
                category = StreakCategory.fromName(entity.category),
                color = entity.color,
                creationDate = entity.creationDate,
                startDate = if (entity.startDate.isNotEmpty()) entity.startDate else entity.creationDate,
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
                nextReminderTimeMillis = entity.nextReminderTimeMillis,
                updatedAt = entity.updatedAt
            )
        }
    }
}
