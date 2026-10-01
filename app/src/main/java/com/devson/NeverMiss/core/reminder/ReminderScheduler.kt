package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StreakRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNextAlarm(streakId: Long) {
        val streak = repository.getStreakById(streakId) ?: return
        if (streak.isArchived || streak.isPaused) {
            cancelAlarm(streakId)
            return
        }

        val nextReminderDateTime = calculateNextReminderDateTime(streak)
        if (nextReminderDateTime == null) {
            cancelAlarm(streakId)
            return
        }

        val triggerAtMillis = nextReminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Cache next reminder time in database
        if (streak.nextReminderTimeMillis != triggerAtMillis) {
            repository.updateStreak(streak.copy(nextReminderTimeMillis = triggerAtMillis))
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.devson.NeverMiss.ACTION_REMINDER_${streakId}"
            putExtra("STREAK_ID", streakId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            streakId.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for security restrictions on exact alarms
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    suspend fun scheduleAllActiveReminders() {
        val activeStreaks = repository.getActiveStreaks()
        for (streak in activeStreaks) {
            scheduleNextAlarm(streak.id)
        }
    }

    fun cancelAlarm(streakId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.devson.NeverMiss.ACTION_REMINDER_${streakId}"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            streakId.toInt(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextReminderDateTime(
        streak: Streak,
        nowDateTime: LocalDateTime = AppClock.nowDateTime()
    ): LocalDateTime? {
        val nowDate = nowDateTime.toLocalDate()
        val nowTime = nowDateTime.toLocalTime()

        val reminderLocalTime = parseLocalTime(streak.reminderTime)
        val todayStr = StreakCalculator.formatDate(nowDate)
        val isTodayCompleted = streak.lastCompletedDate == todayStr
        val isTodayScheduled = StreakCalculator.isScheduled(streak, nowDate)

        // If scheduled today, not completed yet, and reminder time is in the future
        if (isTodayScheduled && !isTodayCompleted && nowTime.isBefore(reminderLocalTime)) {
            return nowDate.atTime(reminderLocalTime)
        }

        // Otherwise, schedule for the next upcoming scheduled day starting from tomorrow
        val nextDate = findNextScheduledDate(streak, nowDate.plusDays(1))
        return nextDate.atTime(reminderLocalTime)
    }

    private fun findNextScheduledDate(streak: Streak, startDate: LocalDate): LocalDate {
        var date = startDate
        for (i in 0..30) {
            if (StreakCalculator.isScheduled(streak, date)) {
                return date
            }
            date = date.plusDays(1)
        }
        return startDate
    }

    private fun parseLocalTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            LocalTime.of(10, 0)
        }
    }
}
