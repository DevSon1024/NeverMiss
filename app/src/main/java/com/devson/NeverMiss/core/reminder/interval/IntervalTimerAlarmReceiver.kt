package com.devson.NeverMiss.core.reminder.interval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.core.util.TimeProvider
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import com.devson.NeverMiss.domain.usecase.intervaltimer.AdvanceIntervalTimerOccurrenceUseCase
import com.devson.NeverMiss.domain.usecase.intervaltimer.CompleteIntervalTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntervalTimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: IntervalTimerRepository

    @Inject
    lateinit var advanceOccurrenceUseCase: AdvanceIntervalTimerOccurrenceUseCase

    @Inject
    lateinit var completeTimerUseCase: CompleteIntervalTimerUseCase

    @Inject
    lateinit var notificationHelper: IntervalTimerNotificationHelper

    @Inject
    lateinit var timeProvider: TimeProvider

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
                processAlarm(timerId)
            } catch (_: Throwable) {
                // Defensive degradation, prevent unhandled exception
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun processAlarm(timerId: Long) {
        val now = timeProvider.currentTimeMillis()

        // 1. Fetch latest timer state from repository
        val timer = repository.getTimerById(timerId) ?: return

        // 2. Validate timer status
        if (timer.status != IntervalTimerStatus.RUNNING) {
            return
        }

        // 3. Validate trigger timestamp
        val scheduledTrigger = timer.nextTriggerAtMillis ?: return
        // Allow a small 500ms tolerance for clock jitter, but reject clearly premature triggers
        if (now < scheduledTrigger - 500L) {
            return
        }

        // 4. Process occurrence based on mode
        when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> {
                // Complete the countdown timer
                val completed = completeTimerUseCase(timerId, now)
                if (completed) {
                    val updatedTimer = repository.getTimerById(timerId)
                    if (updatedTimer != null && updatedTimer.status == IntervalTimerStatus.COMPLETED) {
                        notificationHelper.showTimerNotification(updatedTimer)
                    }
                }
            }
            IntervalTimerMode.INTERVAL -> {
                // Advance repeating occurrence
                val advanced = advanceOccurrenceUseCase(timerId, now)
                if (advanced) {
                    val updatedTimer = repository.getTimerById(timerId)
                    if (updatedTimer != null) {
                        notificationHelper.showTimerNotification(updatedTimer)
                    }
                }
            }
        }
    }
}
