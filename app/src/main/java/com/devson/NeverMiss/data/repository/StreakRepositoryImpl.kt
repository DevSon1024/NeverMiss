package com.devson.NeverMiss.data.repository

import com.devson.NeverMiss.data.local.db.StreakDao
import com.devson.NeverMiss.domain.model.CompletionHistory
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao
) : StreakRepository {

    override fun getActiveStreaksFlow(): Flow<List<Streak>> {
        return streakDao.getAllActiveStreaksFlow().map { entities ->
            entities.map { Streak.fromEntity(it) }
        }
    }

    override suspend fun getActiveStreaks(): List<Streak> {
        return streakDao.getAllActiveStreaks().map { Streak.fromEntity(it) }
    }

    override fun getAllArchivedStreaksFlow(): Flow<List<Streak>> {
        return streakDao.getAllArchivedStreaksFlow().map { entities ->
            entities.map { Streak.fromEntity(it) }
        }
    }

    override fun getAllStreaksFlow(): Flow<List<Streak>> {
        return streakDao.getAllStreaksFlow().map { entities ->
            entities.map { Streak.fromEntity(it) }
        }
    }

    override fun getStreakByIdFlow(id: Long): Flow<Streak?> {
        return streakDao.getStreakByIdFlow(id).map { entity ->
            entity?.let { Streak.fromEntity(it) }
        }
    }

    override suspend fun getStreakById(id: Long): Streak? {
        return streakDao.getStreakById(id)?.let { Streak.fromEntity(it) }
    }

    override suspend fun insertStreak(streak: Streak): Long {
        return streakDao.insertStreak(streak.toEntity())
    }

    override suspend fun updateStreak(streak: Streak) {
        streakDao.updateStreak(streak.toEntity())
    }

    override suspend fun deleteStreak(streak: Streak) {
        streakDao.deleteStreak(streak.toEntity())
    }

    override fun getHistoryForStreakFlow(streakId: Long): Flow<List<CompletionHistory>> {
        return streakDao.getHistoryForStreakFlow(streakId).map { entities ->
            entities.map { CompletionHistory.fromEntity(it) }
        }
    }

    override suspend fun getHistoryForStreak(streakId: Long): List<CompletionHistory> {
        return streakDao.getHistoryForStreak(streakId).map { CompletionHistory.fromEntity(it) }
    }

    override suspend fun getHistoryForAllStreaksOnDate(date: String): List<CompletionHistory> {
        return streakDao.getHistoryForAllStreaksOnDate(date).map { CompletionHistory.fromEntity(it) }
    }

    override suspend fun getHistoryForStreakOnDate(streakId: Long, date: String): CompletionHistory? {
        return streakDao.getHistoryForStreakOnDate(streakId, date)?.let { CompletionHistory.fromEntity(it) }
    }

    override suspend fun logCompletionState(streakId: Long, date: String, status: String, timestamp: Long) {
        streakDao.logCompletionState(streakId, date, status, timestamp)
    }

    override suspend fun clearHistoryForStreak(streakId: Long) {
        streakDao.clearHistoryForStreak(streakId)
    }
}
