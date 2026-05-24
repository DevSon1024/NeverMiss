package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.usecase.LogCompletionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logCompletionUseCase: LogCompletionUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var userPreferences: UserPreferences

    companion object {
        const val ACTION_DONE = "com.devson.NeverMiss.ACTION_DONE"
        const val ACTION_SNOOZE = "com.devson.NeverMiss.ACTION_SNOOZE"
        const val ACTION_SKIP = "com.devson.NeverMiss.ACTION_SKIP"
        const val ACTION_FREEZE = "com.devson.NeverMiss.ACTION_FREEZE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val streakId = intent.getLongExtra("STREAK_ID", -1L)
        if (streakId == -1L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val todayStr = StreakCalculator.formatDate(com.devson.NeverMiss.core.util.AppClock.now())
                notificationHelper.cancelNotification(streakId)

                when (action) {
                    ACTION_DONE -> {
                        logCompletionUseCase(streakId, todayStr, CompletionStatus.COMPLETED)
                    }
                    ACTION_SKIP -> {
                        logCompletionUseCase(streakId, todayStr, CompletionStatus.SKIPPED)
                    }
                    ACTION_FREEZE -> {
                        val success = logCompletionUseCase(streakId, todayStr, CompletionStatus.FROZEN)
                        if (!success) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "No freezes available!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ACTION_SNOOZE -> {
                        val snoozeMins = userPreferences.snoozeDurationMinutesFlow.first()
                        val triggerAtMillis = System.currentTimeMillis() + (snoozeMins * 60 * 1000)

                        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                            this.action = "com.devson.NeverMiss.ACTION_ALARM_${streakId}"
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
                            alarmIntent,
                            flags
                        )

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        
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

                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Snoozed for $snoozeMins minutes", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
