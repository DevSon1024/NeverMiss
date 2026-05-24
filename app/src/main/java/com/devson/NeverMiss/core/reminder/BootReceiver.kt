package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: StreakRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val activeStreaks = repository.getActiveStreaks()
                    for (streak in activeStreaks) {
                        reminderScheduler.scheduleNextAlarm(streak.id)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
