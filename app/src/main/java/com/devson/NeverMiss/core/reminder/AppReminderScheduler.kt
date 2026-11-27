package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStreakRepository: AppStreakRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNextAppAlarm(
        appId: Long,
        minBaseDateTime: LocalDateTime = AppClock.nowDateTime()
    ) {
        val app = appStreakRepository.getTrackedAppById(appId) ?: return
        if (app.isArchived || app.isPaused || !app.isEnabled || app.reminderTime.isBlank()) {
            cancelAppAlarm(appId)
            return
        }

        val todayStr = StreakCalculator.formatDate(minBaseDateTime.toLocalDate())
        val accounts = appStreakRepository.getAccountsForApp(appId)
        val activeAccounts = accounts.filter { it.isEnabled && !it.isArchived }
        val isCompletedToday = when {
            activeAccounts.size > 1 -> activeAccounts.count { it.lastCompletedDate == todayStr } >= activeAccounts.size
            activeAccounts.size == 1 -> activeAccounts.first().lastCompletedDate == todayStr || app.lastCompletedDate == todayStr
            else -> app.lastCompletedDate == todayStr
        }

        val nextDateTime = calculateNextAppReminderDateTime(app, isCompletedToday, minBaseDateTime)
        val triggerAtMillis = nextDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.devson.NeverMiss.ACTION_APP_REMINDER_$appId"
            putExtra("APP_ID", appId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100_000 + appId.toInt(),
            intent,
            flags
        )

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
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
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    suspend fun scheduleAllActiveAppReminders() {
        val activeApps = appStreakRepository.getActiveTrackedAppsFlow().first()
        for (app in activeApps) {
            if (app.reminderTime.isNotBlank()) {
                scheduleNextAppAlarm(app.id)
            }
        }
    }

    fun cancelAppAlarm(appId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.devson.NeverMiss.ACTION_APP_REMINDER_$appId"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100_000 + appId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextAppReminderDateTime(
        app: TrackedApp,
        isCompletedToday: Boolean,
        nowDateTime: LocalDateTime = AppClock.nowDateTime()
    ): LocalDateTime {
        val nowDate = nowDateTime.toLocalDate()
        val nowTime = nowDateTime.toLocalTime()
        val reminderLocalTime = try {
            LocalTime.parse(app.reminderTime)
        } catch (_: Exception) {
            LocalTime.of(10, 0)
        }

        return if (!isCompletedToday && nowTime.isBefore(reminderLocalTime)) {
            nowDate.atTime(reminderLocalTime)
        } else {
            nowDate.plusDays(1).atTime(reminderLocalTime)
        }
    }
}
