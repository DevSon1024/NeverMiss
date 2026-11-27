package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devson.NeverMiss.core.apptracking.worker.AppTrackingScheduler
import com.devson.NeverMiss.core.reminder.interval.IntervalTimerRecoveryCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var appReminderScheduler: AppReminderScheduler

    @Inject
    lateinit var appTrackingScheduler: AppTrackingScheduler

    @Inject
    lateinit var intervalTimerRecoveryCoordinator: IntervalTimerRecoveryCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isExactAlarmAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        } else {
            false
        }

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            isExactAlarmAction
        ) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    // 1. Existing streak reminders & app tracking reconciliation
                    reminderScheduler.scheduleAllActiveReminders()
                    appReminderScheduler.scheduleAllActiveAppReminders()
                    appTrackingScheduler.schedulePeriodicReconciliation()

                    // 2. Interval timer recovery (only when device storage is unlocked, avoiding Direct Boot crashes)
                    intervalTimerRecoveryCoordinator.recoverActiveTimers()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
