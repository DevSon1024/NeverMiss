package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: StreakRepository

    @Inject
    lateinit var appStreakRepository: AppStreakRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var appReminderScheduler: AppReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val streakId = intent.getLongExtra("STREAK_ID", -1L)
        val appId = intent.getLongExtra("APP_ID", -1L)
        if (streakId == -1L && appId == -1L) return

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NeverMiss:StreakAlarmWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(15_000L)
        }

        scope.launch {
            try {
                val todayStr = StreakCalculator.formatDate(AppClock.now())

                if (streakId > 0L) {
                    val streak = repository.getStreakById(streakId)
                    if (streak != null && !streak.isArchived && !streak.isPaused) {
                        val isCompleted = streak.lastCompletedDate == todayStr
                        if (!isCompleted) {
                            notificationHelper.showReminderNotification(streak)
                        }
                        reminderScheduler.scheduleNextAlarm(
                            streakId = streakId,
                            minBaseDateTime = AppClock.nowDateTime().plusMinutes(1)
                        )
                    }
                }

                if (appId > 0L) {
                    val app = appStreakRepository.getTrackedAppById(appId)
                    if (app != null && !app.isArchived && !app.isPaused && app.isEnabled && app.reminderTime.isNotBlank()) {
                        val accounts = appStreakRepository.getAccountsForApp(appId)
                        val activeAccounts = accounts.filter { it.isEnabled && !it.isArchived }
                        val isCompletedToday = when {
                            activeAccounts.size > 1 -> activeAccounts.count { it.lastCompletedDate == todayStr } >= activeAccounts.size
                            activeAccounts.size == 1 -> activeAccounts.first().lastCompletedDate == todayStr || app.lastCompletedDate == todayStr
                            else -> app.lastCompletedDate == todayStr
                        }
                        if (!isCompletedToday) {
                            notificationHelper.showAppReminderNotification(app)
                        }
                        appReminderScheduler.scheduleNextAppAlarm(
                            appId = appId,
                            minBaseDateTime = AppClock.nowDateTime().plusMinutes(1)
                        )
                    }
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Throwable) {
                }
                pendingResult.finish()
            }
        }
    }
}
