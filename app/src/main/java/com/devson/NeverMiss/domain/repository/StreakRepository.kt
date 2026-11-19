package com.devson.NeverMiss.domain.repository

import com.devson.NeverMiss.domain.model.CompletionHistory
import com.devson.NeverMiss.domain.model.Streak
import kotlinx.coroutines.flow.Flow

interface StreakRepository {
    
    // Streaks
    fun getActiveStreaksFlow(): Flow<List<Streak>>
    
    suspend fun getActiveStreaks(): List<Streak>

    fun getAllArchivedStreaksFlow(): Flow<List<Streak>>
    
    fun getAllStreaksFlow(): Flow<List<Streak>>
    
    fun getStreakByIdFlow(id: Long): Flow<Streak?>
    
    suspend fun getStreakById(id: Long): Streak?
    
    suspend fun insertStreak(streak: Streak): Long
    
    suspend fun updateStreak(streak: Streak)
    
    suspend fun deleteStreak(streak: Streak)
    
    // Completion History
    fun getHistoryForStreakFlow(streakId: Long): Flow<List<CompletionHistory>>
    
    suspend fun getHistoryForStreak(streakId: Long): List<CompletionHistory>
    
    suspend fun getHistoryForAllStreaksOnDate(date: String): List<CompletionHistory>
    
    suspend fun getHistoryForStreakOnDate(streakId: Long, date: String): CompletionHistory?
    
    suspend fun logCompletionState(streakId: Long, date: String, status: String, timestamp: Long)
    
    suspend fun clearHistoryForStreak(streakId: Long)
}
