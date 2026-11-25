package com.devson.NeverMiss.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalTimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: IntervalTimerEntity): Long

    @Update
    suspend fun updateTimer(timer: IntervalTimerEntity)

    @Query("DELETE FROM interval_timers WHERE id = :id")
    suspend fun deleteTimerById(id: Long): Int

    @Query("SELECT * FROM interval_timers WHERE id = :id")
    suspend fun getTimerById(id: Long): IntervalTimerEntity?

    @Query("SELECT * FROM interval_timers WHERE id = :id")
    fun observeTimerById(id: Long): Flow<IntervalTimerEntity?>

    @Query("SELECT * FROM interval_timers ORDER BY createdAtMillis DESC")
    fun observeAllTimers(): Flow<List<IntervalTimerEntity>>

    @Query("SELECT * FROM interval_timers WHERE status = :status ORDER BY createdAtMillis DESC")
    fun observeTimersByStatus(status: String): Flow<List<IntervalTimerEntity>>

    @Query(
        "SELECT * FROM interval_timers WHERE status = :runningStatus AND nextTriggerAtMillis IS NOT NULL ORDER BY nextTriggerAtMillis ASC"
    )
    suspend fun getTimersEligibleForRecovery(runningStatus: String): List<IntervalTimerEntity>
}
