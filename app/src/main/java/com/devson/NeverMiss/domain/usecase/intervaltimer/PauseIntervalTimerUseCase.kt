package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.core.reminder.interval.IntervalTimerAlarmScheduler
import com.devson.NeverMiss.core.util.TimeProvider
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerStateMachine
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class PauseIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository,
    private val timeProvider: TimeProvider,
    private val alarmScheduler: IntervalTimerAlarmScheduler
) {
    suspend operator fun invoke(id: Long): Boolean {
        return invoke(id, timeProvider.currentTimeMillis())
    }

    suspend operator fun invoke(id: Long, nowMillis: Long): Boolean {
        if (id <= 0L) return false
        val existing = repository.getTimerById(id) ?: return false
        val updated = IntervalTimerStateMachine.pause(existing, nowMillis)
        repository.updateTimer(updated)
        alarmScheduler.cancel(id)
        return true
    }

    operator fun invoke(timer: IntervalTimer): IntervalTimer {
        return invoke(timer, timeProvider.currentTimeMillis())
    }

    operator fun invoke(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        return IntervalTimerStateMachine.pause(timer, nowMillis)
    }
}
