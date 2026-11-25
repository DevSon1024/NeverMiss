package com.devson.NeverMiss.core.reminder.interval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntervalTimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmProcessor: IntervalTimerAlarmProcessor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!action.startsWith(IntervalTimerAlarmContract.ACTION_INTERVAL_TIMER_ALARM)) {
            return
        }

        val timerId = intent.getLongExtra(IntervalTimerAlarmContract.EXTRA_TIMER_ID, -1L)
        if (timerId <= 0L) {
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                alarmProcessor.processDueAlarm(timerId)
            } catch (_: Throwable) {
                // Defensive degradation, prevent unhandled exception
            } finally {
                pendingResult.finish()
            }
        }
    }
}
