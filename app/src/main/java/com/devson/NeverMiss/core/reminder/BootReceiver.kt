package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.core.apptracking.worker.AppTrackingScheduler
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
    lateinit var appTrackingScheduler: AppTrackingScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    reminderScheduler.scheduleAllActiveReminders()
                    appTrackingScheduler.schedulePeriodicReconciliation()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
