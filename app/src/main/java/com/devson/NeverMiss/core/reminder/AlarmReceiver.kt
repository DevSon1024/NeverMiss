package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

                    // Reschedule next recurrence for tomorrow / next scheduled day
                    reminderScheduler.scheduleNextAlarm(streakId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
