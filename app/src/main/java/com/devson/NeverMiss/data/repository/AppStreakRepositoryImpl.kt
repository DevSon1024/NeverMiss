package com.devson.NeverMiss.data.repository

import com.devson.NeverMiss.data.local.db.apptracking.AppStreakCompletionEntity
import com.devson.NeverMiss.data.local.db.apptracking.AppStreakDao
import com.devson.NeverMiss.data.local.db.apptracking.PendingConfirmationEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAccountEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAppEntity
import com.devson.NeverMiss.data.local.db.apptracking.UsageCheckpointEntity
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStreakRepositoryImpl @Inject constructor(
    private val appStreakDao: AppStreakDao
) : AppStreakRepository {

    // --- Tracked Apps ---

    override fun getAllTrackedAppsFlow(): Flow<List<TrackedApp>> {
        return appStreakDao.getAllTrackedAppsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTrackedAppsFlow(): Flow<List<TrackedApp>> {
        return appStreakDao.getActiveTrackedAppsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getArchivedTrackedAppsFlow(): Flow<List<TrackedApp>> {
        return appStreakDao.getArchivedTrackedAppsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllTrackedApps(): List<TrackedApp> {
        return appStreakDao.getAllTrackedApps().map { it.toDomain() }
    }

    override suspend fun getEnabledTrackedApps(): List<TrackedApp> {
        return appStreakDao.getEnabledTrackedApps().map { it.toDomain() }
    }

    override suspend fun getTrackedAppById(id: Long): TrackedApp? {
        return appStreakDao.getTrackedAppById(id)?.toDomain()
    }

    override fun getTrackedAppByIdFlow(id: Long): Flow<TrackedApp?> {
        return appStreakDao.getTrackedAppByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getTrackedAppByPackage(packageName: String): TrackedApp? {
        return appStreakDao.getTrackedAppByPackage(packageName)?.toDomain()
    }

    override suspend fun addTrackedApp(app: TrackedApp): Long {
        return appStreakDao.insertTrackedApp(TrackedAppEntity.fromDomain(app))
    }

    override suspend fun updateTrackedApp(app: TrackedApp) {
        appStreakDao.updateTrackedApp(TrackedAppEntity.fromDomain(app))
    }

    override suspend fun deleteTrackedApp(app: TrackedApp) {
        appStreakDao.deleteTrackedApp(TrackedAppEntity.fromDomain(app))
    }

    override suspend fun deleteTrackedAppById(id: Long) {
        appStreakDao.deleteTrackedAppById(id)
    }

    // --- Tracked Accounts ---

    override fun getAllAccountsFlow(): Flow<List<TrackedAccount>> {
        return appStreakDao.getAllAccountsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountsForAppFlow(appId: Long): Flow<List<TrackedAccount>> {
        return appStreakDao.getAccountsForAppFlow(appId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountsForApp(appId: Long): List<TrackedAccount> {
        return appStreakDao.getAccountsForApp(appId).map { it.toDomain() }
    }

    override suspend fun getEnabledAccountsForApp(appId: Long): List<TrackedAccount> {
        return appStreakDao.getEnabledAccountsForApp(appId).map { it.toDomain() }
    }

    override suspend fun getAccountById(id: Long): TrackedAccount? {
        return appStreakDao.getAccountById(id)?.toDomain()
    }

    override fun getAccountByIdFlow(id: Long): Flow<TrackedAccount?> {
        return appStreakDao.getAccountByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addAccount(account: TrackedAccount): Long {
        return appStreakDao.insertAccount(TrackedAccountEntity.fromDomain(account))
    }

    override suspend fun updateAccount(account: TrackedAccount) {
        appStreakDao.updateAccount(TrackedAccountEntity.fromDomain(account))
    }

    override suspend fun deleteAccount(account: TrackedAccount) {
        appStreakDao.deleteAccount(TrackedAccountEntity.fromDomain(account))
    }

    // --- Completions ---

    override fun getCompletionsForAppFlow(appId: Long): Flow<List<AppStreakCompletion>> {
        return appStreakDao.getCompletionsForAppFlow(appId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCompletionsForApp(appId: Long): List<AppStreakCompletion> {
        return appStreakDao.getCompletionsForApp(appId).map { it.toDomain() }
    }

    override fun getCompletionsForAccountFlow(accountId: Long): Flow<List<AppStreakCompletion>> {
        return appStreakDao.getCompletionsForAccountFlow(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCompletionsForAccount(accountId: Long): List<AppStreakCompletion> {
        return appStreakDao.getCompletionsForAccount(accountId).map { it.toDomain() }
    }

    override suspend fun isAppCompletedOnDate(appId: Long, date: String): Boolean {
        return appStreakDao.getAppCompletionForDate(appId, date) != null
    }

    override suspend fun isAccountCompletedOnDate(accountId: Long, date: String): Boolean {
        return appStreakDao.getAccountCompletionForDate(accountId, date) != null
    }

    override suspend fun logAppCompletion(
        appId: Long,
        date: String,
        timestamp: Long,
        method: CompletionMethod
    ): Boolean {
        return appStreakDao.recordAppUsageCompletion(
            appId = appId,
            date = date,
            timestamp = timestamp,
            method = method.name
        )
    }

    override suspend fun logAccountCompletion(
        appId: Long,
        accountId: Long,
        date: String,
        timestamp: Long,
        method: CompletionMethod
    ): Boolean {
        return appStreakDao.recordAccountCompletion(
            appId = appId,
            accountId = accountId,
            date = date,
            timestamp = timestamp,
            method = method.name
        )
    }

    // --- Usage Checkpoints ---

    override suspend fun getCheckpoint(source: String): UsageCheckpoint? {
        return appStreakDao.getCheckpoint(source)?.toDomain()
    }

    override suspend fun saveCheckpoint(source: String, timestamp: Long) {
        val existing = appStreakDao.getCheckpoint(source)
        val entity = UsageCheckpointEntity(
            id = existing?.id ?: 0L,
            sourceIdentifier = source,
            lastProcessedTimestamp = timestamp,
            updatedAt = System.currentTimeMillis()
        )
        appStreakDao.insertOrReplaceCheckpoint(entity)
    }

    // --- Pending Account Confirmations ---

    override fun getActivePendingConfirmationsFlow(nowMillis: Long): Flow<List<PendingConfirmation>> {
        return appStreakDao.getActivePendingConfirmationsFlow(nowMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPendingConfirmationsForApp(appId: Long, nowMillis: Long): List<PendingConfirmation> {
        return appStreakDao.getPendingConfirmationsForApp(appId, nowMillis).map { it.toDomain() }
    }

    override suspend fun getPendingConfirmationById(id: Long): PendingConfirmation? {
        return appStreakDao.getPendingConfirmationById(id)?.toDomain()
    }

    override suspend fun addPendingConfirmation(confirmation: PendingConfirmation): Long {
        return appStreakDao.insertPendingConfirmation(PendingConfirmationEntity.fromDomain(confirmation))
    }

    override suspend fun updatePendingConfirmation(confirmation: PendingConfirmation) {
        appStreakDao.updatePendingConfirmation(PendingConfirmationEntity.fromDomain(confirmation))
    }

    override suspend fun expireOldConfirmations(nowMillis: Long): Int {
        return appStreakDao.expireOldConfirmations(nowMillis)
    }
}
