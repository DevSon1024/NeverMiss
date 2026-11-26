package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
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
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val streakId = intent.getLongExtra("STREAK_ID", -1L)
        if (streakId == -1L) return

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
                val streak = repository.getStreakById(streakId)
                if (streak != null && !streak.isArchived && !streak.isPaused) {
                    val today = AppClock.now()
                    val todayStr = StreakCalculator.formatDate(today)

                    val isCompleted = streak.lastCompletedDate == todayStr

                    if (!isCompleted) {
                        notificationHelper.showReminderNotification(streak)
                    }

                    // Reschedule next recurrence with 1-minute buffer to prevent clock-jitter double-fires
                    reminderScheduler.scheduleNextAlarm(
                        streakId = streakId,
                        minBaseDateTime = AppClock.nowDateTime().plusMinutes(1)
                    )
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
