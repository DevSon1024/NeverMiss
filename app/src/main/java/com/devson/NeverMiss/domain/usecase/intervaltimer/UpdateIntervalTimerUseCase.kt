package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class UpdateIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    suspend operator fun invoke(updatedTimer: IntervalTimer): Boolean {
        return invoke(updatedTimer, System.currentTimeMillis())
    }

    suspend operator fun invoke(
        updatedTimer: IntervalTimer,
        currentTimeMillis: Long
    ): Boolean {
        if (updatedTimer.id <= 0L) return false
        val existing = repository.getTimerById(updatedTimer.id) ?: return false

        val timerToSave = when (existing.status) {
            IntervalTimerStatus.RUNNING, IntervalTimerStatus.PAUSED -> {
                require(updatedTimer.mode == existing.mode) {
                    "Cannot change timer mode while timer is ${existing.status}."
                }
                require(updatedTimer.durationMillis == existing.durationMillis) {
                    "Cannot change countdown duration while timer is ${existing.status}."
                }
                require(updatedTimer.intervalMillis == existing.intervalMillis) {
                    "Cannot change interval duration while timer is ${existing.status}."
                }
                existing.copy(
                    title = updatedTimer.title.trim(),
                    repeatLimit = updatedTimer.repeatLimit,
                    updatedAtMillis = maxOf(currentTimeMillis, existing.createdAtMillis)
                )
            }
            IntervalTimerStatus.IDLE, IntervalTimerStatus.STOPPED, IntervalTimerStatus.COMPLETED -> {
                updatedTimer.copy(
                    id = existing.id,
                    title = updatedTimer.title.trim(),
                    createdAtMillis = existing.createdAtMillis,
                    updatedAtMillis = maxOf(currentTimeMillis, existing.createdAtMillis),
                    startedAtMillis = null,
                    nextTriggerAtMillis = null,
                    remainingAtPauseMillis = null
                )
            }
        }

        timerToSave.validate()
        repository.updateTimer(timerToSave)
        return true
    }

    suspend fun updateConfiguration(
        id: Long,
        title: String,
        mode: IntervalTimerMode,
        durationMillis: Long = 0L,
        intervalMillis: Long = 0L,
        repeatLimit: Int? = null
    ): Boolean {
        return updateConfiguration(
            id = id,
            title = title,
            mode = mode,
            durationMillis = durationMillis,
            intervalMillis = intervalMillis,
            repeatLimit = repeatLimit,
            currentTimeMillis = System.currentTimeMillis()
        )
    }

    suspend fun updateConfiguration(
        id: Long,
        title: String,
        mode: IntervalTimerMode,
        durationMillis: Long,
        intervalMillis: Long,
        repeatLimit: Int?,
        currentTimeMillis: Long
    ): Boolean {
        if (id <= 0L) return false
        val existing = repository.getTimerById(id) ?: return false
        val updatedCandidate = existing.copy(
            title = title,
            mode = mode,
            durationMillis = durationMillis,
            intervalMillis = intervalMillis,
            repeatLimit = repeatLimit
        )
        return invoke(updatedCandidate, currentTimeMillis)
    }
}
