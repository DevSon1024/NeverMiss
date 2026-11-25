package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.core.reminder.interval.IntervalTimerAlarmScheduler
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class DeleteIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository,
    private val alarmScheduler: IntervalTimerAlarmScheduler
) {
    suspend operator fun invoke(id: Long): Boolean {
        if (id <= 0L) return false
        val deleted = repository.deleteTimerById(id)
        if (deleted) {
            alarmScheduler.cancel(id)
        }
        return deleted
    }
}
