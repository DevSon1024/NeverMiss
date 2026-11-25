package com.devson.NeverMiss.domain.repository

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import kotlinx.coroutines.flow.Flow

interface IntervalTimerRepository {

    suspend fun insertTimer(timer: IntervalTimer): Long

    suspend fun updateTimer(timer: IntervalTimer)

    suspend fun deleteTimerById(id: Long): Boolean

    suspend fun getTimerById(id: Long): IntervalTimer?

    fun observeTimerById(id: Long): Flow<IntervalTimer?>

    fun observeAllTimers(): Flow<List<IntervalTimer>>

    fun observeTimersByStatus(status: IntervalTimerStatus): Flow<List<IntervalTimer>>

    suspend fun getTimersEligibleForRecovery(): List<IntervalTimer>
}
