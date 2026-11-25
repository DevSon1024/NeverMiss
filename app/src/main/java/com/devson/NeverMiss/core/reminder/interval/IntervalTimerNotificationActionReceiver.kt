package com.devson.NeverMiss.core.reminder.interval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.domain.usecase.intervaltimer.SnoozeIntervalTimerUseCase
import com.devson.NeverMiss.domain.usecase.intervaltimer.StopIntervalTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntervalTimerNotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stopTimerUseCase: StopIntervalTimerUseCase

    @Inject
    lateinit var snoozeTimerUseCase: SnoozeIntervalTimerUseCase

    @Inject
    lateinit var notificationHelper: IntervalTimerNotificationHelper

    @Inject
    lateinit var alertManager: IntervalTimerAlertManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val rawAction = intent.action ?: return
        val timerId = intent.getLongExtra(IntervalTimerNotificationHelper.EXTRA_INTERVAL_TIMER_ID, -1L)
        if (timerId <= 0L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                // Dismiss or cancel notification first for immediate responsiveness
                notificationHelper.cancelNotification(timerId)
                alertManager.stopAlert()

                when {
                    rawAction.startsWith(IntervalTimerNotificationHelper.ACTION_INTERVAL_TIMER_DONE) -> {
                        // Acknowledge occurrence: notification is dismissed, interval schedule continues undisturbed
                    }
                    rawAction.startsWith(IntervalTimerNotificationHelper.ACTION_INTERVAL_TIMER_DISMISS) -> {
                        // Dismiss completed timer alert
                    }
                    rawAction.startsWith(IntervalTimerNotificationHelper.ACTION_INTERVAL_TIMER_STOP) -> {
                        // Stop timer lifecycle: updates status to STOPPED, cancels pending alarms
                        stopTimerUseCase(timerId)
                    }
                    rawAction.startsWith(IntervalTimerNotificationHelper.ACTION_INTERVAL_TIMER_SNOOZE) -> {
                        val customSnooze = if (intent.hasExtra(IntervalTimerNotificationHelper.EXTRA_SNOOZE_MILLIS)) {
                            intent.getLongExtra(IntervalTimerNotificationHelper.EXTRA_SNOOZE_MILLIS, 5 * 60 * 1000L)
                        } else {
                            null
                        }
                        snoozeTimerUseCase(timerId, customSnoozeMillis = customSnooze)
                    }
                }
            } catch (_: Throwable) {
                // Defensive degradation
            } finally {
                pendingResult.finish()
            }
        }
    }
}
