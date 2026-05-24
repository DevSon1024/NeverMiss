package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
            action = "com.devson.NeverMiss.ACTION_ALARM_${streakId}"
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
    }

    fun cancelAlarm(streakId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.devson.NeverMiss.ACTION_ALARM_${streakId}"
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
        nowDateTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        val nowDate = nowDateTime.toLocalDate()
        val nowTime = nowDateTime.toLocalTime()

        // 1. Check if today is completed
        val todayStr = StreakCalculator.formatDate(nowDate)
        val isTodayCompleted = streak.lastCompletedDate == todayStr

        if (isTodayCompleted) {
            // Task is done today. Find next scheduled day.
            val nextDate = findNextScheduledDate(streak, nowDate.plusDays(1))
            val reminderLocalTime = parseLocalTime(streak.reminderTime)
            return nextDate.atTime(reminderLocalTime)
        }

        // Today is not completed. Check if today is a scheduled day.
        val isTodayScheduled = StreakCalculator.isScheduled(streak, nowDate)

        if (isTodayScheduled) {
            val reminderLocalTime = parseLocalTime(streak.reminderTime)
            if (nowTime.isBefore(reminderLocalTime)) {
                // First reminder for today is in the future
                return nowDate.atTime(reminderLocalTime)
            }

            // It is past the first reminder time. Is repeat enabled?
            if (streak.reminderRepeatHours > 0) {
                // Calculate next repeating alarm time
                val repeatHours = streak.reminderRepeatHours.toLong()
                var nextRepeatTime = reminderLocalTime.plusHours(repeatHours)
                
                while (nextRepeatTime.isBefore(nowTime) || nextRepeatTime == nowTime) {
                    nextRepeatTime = nextRepeatTime.plusHours(repeatHours)
                }

                // Check if this repeat time falls before the cutoff limit
                val cutoffLocalTime = parseLocalTime(streak.cutoffTime)
                val isCutoffNextDay = streak.gracePeriodUntil3AM || cutoffLocalTime.isBefore(reminderLocalTime)

                val hasPassedCutoff = if (isCutoffNextDay) {
                    // Grace period until 3 AM next day
                    val cutoffDateTime = if (streak.gracePeriodUntil3AM) {
                        nowDate.plusDays(1).atTime(LocalTime.of(3, 0))
                    } else {
                        nowDate.plusDays(1).atTime(cutoffLocalTime)
                    }
                    nowDate.atTime(nextRepeatTime).isAfter(cutoffDateTime)
                } else {
                    nextRepeatTime.isAfter(cutoffLocalTime)
                }

                if (!hasPassedCutoff) {
                    return nowDate.atTime(nextRepeatTime)
                }
            }
        }

        // Today is either not scheduled or we ran past the cutoff/reminder times.
        // Schedule for the next scheduled day.
        val nextDate = findNextScheduledDate(streak, nowDate.plusDays(1))
        val reminderLocalTime = parseLocalTime(streak.reminderTime)
        return nextDate.atTime(reminderLocalTime)
    }

    private fun findNextScheduledDate(streak: Streak, startDate: LocalDate): LocalDate {
        var date = startDate
        // Upper bound safety check to avoid infinite loops (up to 30 days)
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
