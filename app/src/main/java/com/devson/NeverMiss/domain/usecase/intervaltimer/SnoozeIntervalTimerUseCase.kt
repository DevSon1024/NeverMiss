package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.core.reminder.interval.IntervalTimerAlarmScheduler
import com.devson.NeverMiss.core.reminder.interval.IntervalTimerSettingsRepository
import com.devson.NeverMiss.core.util.TimeProvider
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SnoozeIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository,
    private val timeProvider: TimeProvider,
    private val alarmScheduler: IntervalTimerAlarmScheduler,
    private val settingsRepository: IntervalTimerSettingsRepository
) {
    suspend operator fun invoke(id: Long, customSnoozeMillis: Long? = null): Boolean {
        val snoozeMillis = customSnoozeMillis ?: try {
            val settings = settingsRepository.settingsFlow.first()
            settings.snoozeDurationMinutes * 60 * 1000L
        } catch (_: Throwable) {
            5 * 60 * 1000L
        }
        return invoke(id, timeProvider.currentTimeMillis(), snoozeMillis)
    }

    suspend operator fun invoke(id: Long, nowMillis: Long, snoozeDurationMillis: Long): Boolean {
        if (id <= 0L || snoozeDurationMillis <= 0L) return false
        val existing = repository.getTimerById(id) ?: return false

        // Snooze is applicable to active RUNNING timers (e.g. interval reminder occurrence or countdown)
        if (existing.status != IntervalTimerStatus.RUNNING) {
            return false
        }

        val newTriggerAtMillis = nowMillis + snoozeDurationMillis
        val safeUpdatedAt = maxOf(nowMillis, existing.createdAtMillis)

        val snoozedTimer = existing.copy(
            nextTriggerAtMillis = newTriggerAtMillis,
            updatedAtMillis = safeUpdatedAt
        )
        snoozedTimer.validate()

        // 1. Persist updated timer state first
        repository.updateTimer(snoozedTimer)

        // 2. Cancel original alarm and schedule new alarm timestamp
        alarmScheduler.cancel(id)
        alarmScheduler.schedule(snoozedTimer)

        return true
    }
}
