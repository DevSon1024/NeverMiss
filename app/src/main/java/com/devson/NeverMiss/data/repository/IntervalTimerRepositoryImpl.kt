package com.devson.NeverMiss.data.repository

import com.devson.NeverMiss.data.local.db.IntervalTimerDao
import com.devson.NeverMiss.data.local.db.IntervalTimerEntity
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerRepositoryImpl @Inject constructor(
    private val intervalTimerDao: IntervalTimerDao
) : IntervalTimerRepository {

    override suspend fun insertTimer(timer: IntervalTimer): Long {
        return intervalTimerDao.insertTimer(timer.toEntity())
    }

    override suspend fun updateTimer(timer: IntervalTimer) {
        intervalTimerDao.updateTimer(timer.toEntity())
    }

    override suspend fun deleteTimerById(id: Long): Boolean {
        return intervalTimerDao.deleteTimerById(id) > 0
    }

    override suspend fun getTimerById(id: Long): IntervalTimer? {
        return intervalTimerDao.getTimerById(id)?.toDomain()
    }

    override fun observeTimerById(id: Long): Flow<IntervalTimer?> {
        return intervalTimerDao.observeTimerById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override fun observeAllTimers(): Flow<List<IntervalTimer>> {
        return intervalTimerDao.observeAllTimers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTimersByStatus(status: IntervalTimerStatus): Flow<List<IntervalTimer>> {
        return intervalTimerDao.observeTimersByStatus(status.toStorageValue()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTimersEligibleForRecovery(): List<IntervalTimer> {
        return intervalTimerDao.getTimersEligibleForRecovery(
            runningStatus = IntervalTimerStatus.RUNNING.toStorageValue()
        ).map { it.toDomain() }
    }
}

internal fun IntervalTimer.toEntity(): IntervalTimerEntity {
    return IntervalTimerEntity(
        id = id,
        title = title,
        mode = mode.toStorageValue(),
        durationMillis = durationMillis,
        intervalMillis = intervalMillis,
        repeatLimit = repeatLimit,
        completedOccurrences = completedOccurrences,
        status = status.toStorageValue(),
        startedAtMillis = startedAtMillis,
        nextTriggerAtMillis = nextTriggerAtMillis,
        remainingAtPauseMillis = remainingAtPauseMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

internal fun IntervalTimerEntity.toDomain(): IntervalTimer {
    return IntervalTimer(
        id = id,
        title = title,
        mode = parseMode(mode),
        durationMillis = durationMillis,
        intervalMillis = intervalMillis,
        repeatLimit = repeatLimit,
        completedOccurrences = completedOccurrences,
        status = parseStatus(status),
        startedAtMillis = startedAtMillis,
        nextTriggerAtMillis = nextTriggerAtMillis,
        remainingAtPauseMillis = remainingAtPauseMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

private fun IntervalTimerMode.toStorageValue(): String = when (this) {
    IntervalTimerMode.COUNTDOWN -> "COUNTDOWN"
    IntervalTimerMode.INTERVAL -> "INTERVAL"
}

private fun parseMode(raw: String): IntervalTimerMode = when (raw) {
    "COUNTDOWN" -> IntervalTimerMode.COUNTDOWN
    "INTERVAL" -> IntervalTimerMode.INTERVAL
    else -> throw IllegalArgumentException("Unknown IntervalTimerMode: $raw")
}

private fun IntervalTimerStatus.toStorageValue(): String = when (this) {
    IntervalTimerStatus.IDLE -> "IDLE"
    IntervalTimerStatus.RUNNING -> "RUNNING"
    IntervalTimerStatus.PAUSED -> "PAUSED"
    IntervalTimerStatus.COMPLETED -> "COMPLETED"
    IntervalTimerStatus.STOPPED -> "STOPPED"
}

private fun parseStatus(raw: String): IntervalTimerStatus = when (raw) {
    "IDLE" -> IntervalTimerStatus.IDLE
    "RUNNING" -> IntervalTimerStatus.RUNNING
    "PAUSED" -> IntervalTimerStatus.PAUSED
    "COMPLETED" -> IntervalTimerStatus.COMPLETED
    "STOPPED" -> IntervalTimerStatus.STOPPED
    else -> throw IllegalArgumentException("Unknown IntervalTimerStatus: $raw")
}
