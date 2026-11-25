package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class CreateIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    suspend operator fun invoke(timer: IntervalTimer): Long {
        val preparedTimer = if (timer.id != 0L) timer.copy(id = 0L) else timer
        preparedTimer.validate()
        return repository.insertTimer(preparedTimer)
    }

    suspend operator fun invoke(
        title: String,
        mode: IntervalTimerMode,
        durationMillis: Long = 0L,
        intervalMillis: Long = 0L,
        repeatLimit: Int? = null
    ): Long {
        return invoke(
            title = title,
            mode = mode,
            durationMillis = durationMillis,
            intervalMillis = intervalMillis,
            repeatLimit = repeatLimit,
            currentTimeMillis = System.currentTimeMillis()
        )
    }

    suspend operator fun invoke(
        title: String,
        mode: IntervalTimerMode,
        durationMillis: Long,
        intervalMillis: Long,
        repeatLimit: Int?,
        currentTimeMillis: Long
    ): Long {
        val timer = IntervalTimer(
            id = 0L,
            title = title.trim(),
            mode = mode,
            durationMillis = durationMillis,
            intervalMillis = intervalMillis,
            repeatLimit = repeatLimit,
            completedOccurrences = 0,
            status = IntervalTimerStatus.IDLE,
            startedAtMillis = null,
            nextTriggerAtMillis = null,
            remainingAtPauseMillis = null,
            createdAtMillis = currentTimeMillis,
            updatedAtMillis = currentTimeMillis
        )
        return invoke(timer)
    }
}
