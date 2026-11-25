package com.devson.NeverMiss.core.reminder.interval

import com.devson.NeverMiss.core.util.TimeProvider
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import com.devson.NeverMiss.domain.usecase.intervaltimer.GetIntervalTimersEligibleForRecoveryUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerRecoveryCoordinator @Inject constructor(
    private val getEligibleTimersUseCase: GetIntervalTimersEligibleForRecoveryUseCase,
    private val repository: IntervalTimerRepository,
    private val alarmScheduler: IntervalTimerAlarmScheduler,
    private val timeProvider: TimeProvider,
    private val alarmProcessor: IntervalTimerAlarmProcessor
) {

    suspend fun recoverActiveTimers(): Int {
        val now = timeProvider.currentTimeMillis()
        val eligibleTimers = try {
            getEligibleTimersUseCase()
        } catch (_: Throwable) {
            return 0
        }

        var recoveredCount = 0

        for (timer in eligibleTimers) {
            try {
                // Re-fetch latest to ensure concurrency safety
                val latest = repository.getTimerById(timer.id) ?: continue
                if (latest.status != IntervalTimerStatus.RUNNING) continue

                val nextTrigger = latest.nextTriggerAtMillis ?: continue

                if (nextTrigger <= now) {
                    // Overdue timer: trigger immediate delivery through existing Phase 5 alarm processor
                    alarmProcessor.processDueAlarm(latest.id, allowImmediateIfDue = true)
                } else {
                    // Future timer: reschedule one-shot alarm via AlarmManager
                    alarmScheduler.schedule(latest)
                }
                recoveredCount++
            } catch (_: Throwable) {
                // Defensive: individual timer failure does not block other timers
            }
        }

        return recoveredCount
    }
}
