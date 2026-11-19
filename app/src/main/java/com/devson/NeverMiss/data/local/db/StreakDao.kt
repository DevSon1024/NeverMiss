package com.devson.NeverMiss.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    // Streaks
    @Query("SELECT * FROM streaks WHERE isArchived = 0 ORDER BY nextReminderTimeMillis ASC")
    fun getAllActiveStreaksFlow(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks WHERE isArchived = 0 ORDER BY nextReminderTimeMillis ASC")
    suspend fun getAllActiveStreaks(): List<StreakEntity>

    @Query("SELECT * FROM streaks WHERE isArchived = 1 ORDER BY creationDate DESC")
    fun getAllArchivedStreaksFlow(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks ORDER BY creationDate DESC")
    fun getAllStreaksFlow(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks WHERE id = :id")
    fun getStreakByIdFlow(id: Long): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE id = :id")
    suspend fun getStreakById(id: Long): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity): Long

    @Update
    suspend fun updateStreak(streak: StreakEntity)

    @Delete
    suspend fun deleteStreak(streak: StreakEntity)

    // Completion History
    @Query("SELECT * FROM completion_history WHERE streakId = :streakId ORDER BY date ASC")
    fun getHistoryForStreakFlow(streakId: Long): Flow<List<CompletionHistoryEntity>>

    @Query("SELECT * FROM completion_history WHERE streakId = :streakId ORDER BY date ASC")
    suspend fun getHistoryForStreak(streakId: Long): List<CompletionHistoryEntity>

    @Query("SELECT * FROM completion_history WHERE date = :date")
    suspend fun getHistoryForAllStreaksOnDate(date: String): List<CompletionHistoryEntity>

    @Query("SELECT * FROM completion_history WHERE streakId = :streakId AND date = :date LIMIT 1")
    suspend fun getHistoryForStreakOnDate(streakId: Long, date: String): CompletionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(history: CompletionHistoryEntity): Long

    @Query("DELETE FROM completion_history WHERE streakId = :streakId AND date = :date")
    suspend fun deleteHistoryEntryOnDate(streakId: Long, date: String)

    @Query("DELETE FROM completion_history WHERE streakId = :streakId")
    suspend fun clearHistoryForStreak(streakId: Long)

    @Transaction
    suspend fun logCompletionState(streakId: Long, date: String, status: String, timestamp: Long) {
        // Remove existing entry on this day if any
        deleteHistoryEntryOnDate(streakId, date)
        if (status.isNotEmpty()) {
            insertHistoryEntry(
                CompletionHistoryEntity(
                    streakId = streakId,
                    date = date,
                    status = status,
                    timestamp = timestamp
                )
            )
        }
    }
}
