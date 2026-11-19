package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStreakDao {

    // --- Tracked Apps ---

    @Query("SELECT * FROM tracked_apps ORDER BY appName ASC")
    fun getAllTrackedAppsFlow(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps ORDER BY appName ASC")
    suspend fun getAllTrackedApps(): List<TrackedAppEntity>

    @Query("SELECT * FROM tracked_apps WHERE isArchived = 0 ORDER BY appName ASC")
    fun getActiveTrackedAppsFlow(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps WHERE isArchived = 1 ORDER BY appName ASC")
    fun getArchivedTrackedAppsFlow(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps WHERE isEnabled = 1 AND isPaused = 0 AND isArchived = 0")
    suspend fun getEnabledTrackedApps(): List<TrackedAppEntity>

    @Query("SELECT * FROM tracked_apps WHERE id = :id LIMIT 1")
    suspend fun getTrackedAppById(id: Long): TrackedAppEntity?

    @Query("SELECT * FROM tracked_apps WHERE id = :id LIMIT 1")
    fun getTrackedAppByIdFlow(id: Long): Flow<TrackedAppEntity?>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getTrackedAppByPackage(packageName: String): TrackedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedApp(app: TrackedAppEntity): Long

    @Update
    suspend fun updateTrackedApp(app: TrackedAppEntity)

    @Delete
    suspend fun deleteTrackedApp(app: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps WHERE id = :id")
    suspend fun deleteTrackedAppById(id: Long)

    // --- Tracked Accounts ---

    @Query("SELECT * FROM tracked_accounts WHERE trackedAppId = :appId ORDER BY accountName ASC")
    fun getAccountsForAppFlow(appId: Long): Flow<List<TrackedAccountEntity>>

    @Query("SELECT * FROM tracked_accounts WHERE trackedAppId = :appId ORDER BY accountName ASC")
    suspend fun getAccountsForApp(appId: Long): List<TrackedAccountEntity>

    @Query("SELECT * FROM tracked_accounts WHERE trackedAppId = :appId AND isEnabled = 1 AND isPaused = 0 AND isArchived = 0")
    suspend fun getEnabledAccountsForApp(appId: Long): List<TrackedAccountEntity>

    @Query("SELECT * FROM tracked_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): TrackedAccountEntity?

    @Query("SELECT * FROM tracked_accounts WHERE id = :id LIMIT 1")
    fun getAccountByIdFlow(id: Long): Flow<TrackedAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: TrackedAccountEntity): Long

    @Update
    suspend fun updateAccount(account: TrackedAccountEntity)

    @Delete
    suspend fun deleteAccount(account: TrackedAccountEntity)

    // --- App Streak Completions ---

    @Query("SELECT * FROM app_streak_completions WHERE trackedAppId = :appId AND accountId IS NULL ORDER BY completionDate ASC")
    fun getCompletionsForAppFlow(appId: Long): Flow<List<AppStreakCompletionEntity>>

    @Query("SELECT * FROM app_streak_completions WHERE trackedAppId = :appId AND accountId IS NULL ORDER BY completionDate ASC")
    suspend fun getCompletionsForApp(appId: Long): List<AppStreakCompletionEntity>

    @Query("SELECT * FROM app_streak_completions WHERE accountId = :accountId ORDER BY completionDate ASC")
    fun getCompletionsForAccountFlow(accountId: Long): Flow<List<AppStreakCompletionEntity>>

    @Query("SELECT * FROM app_streak_completions WHERE accountId = :accountId ORDER BY completionDate ASC")
    suspend fun getCompletionsForAccount(accountId: Long): List<AppStreakCompletionEntity>

    @Query("SELECT * FROM app_streak_completions WHERE trackedAppId = :appId AND accountId IS NULL AND completionDate = :date LIMIT 1")
    suspend fun getAppCompletionForDate(appId: Long, date: String): AppStreakCompletionEntity?

    @Query("SELECT * FROM app_streak_completions WHERE accountId = :accountId AND completionDate = :date LIMIT 1")
    suspend fun getAccountCompletionForDate(accountId: Long, date: String): AppStreakCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: AppStreakCompletionEntity): Long

    @Query("DELETE FROM app_streak_completions WHERE trackedAppId = :appId")
    suspend fun deleteCompletionsForApp(appId: Long)

    @Query("DELETE FROM app_streak_completions WHERE accountId = :accountId")
    suspend fun deleteCompletionsForAccount(accountId: Long)

    // --- Usage Checkpoints ---

    @Query("SELECT * FROM usage_tracking_checkpoints WHERE sourceIdentifier = :source LIMIT 1")
    suspend fun getCheckpoint(source: String): UsageCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceCheckpoint(checkpoint: UsageCheckpointEntity): Long

    // --- Pending Account Confirmations ---

    @Query("SELECT * FROM pending_account_confirmations WHERE status = 'PENDING' AND expiresAt > :nowMillis ORDER BY createdAt DESC")
    fun getActivePendingConfirmationsFlow(nowMillis: Long = System.currentTimeMillis()): Flow<List<PendingConfirmationEntity>>

    @Query("SELECT * FROM pending_account_confirmations WHERE trackedAppId = :appId AND status = 'PENDING' AND expiresAt > :nowMillis ORDER BY createdAt DESC")
    suspend fun getPendingConfirmationsForApp(appId: Long, nowMillis: Long = System.currentTimeMillis()): List<PendingConfirmationEntity>

    @Query("SELECT * FROM pending_account_confirmations WHERE id = :id LIMIT 1")
    suspend fun getPendingConfirmationById(id: Long): PendingConfirmationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingConfirmation(confirmation: PendingConfirmationEntity): Long

    @Update
    suspend fun updatePendingConfirmation(confirmation: PendingConfirmationEntity)

    @Query("UPDATE pending_account_confirmations SET status = 'EXPIRED' WHERE status = 'PENDING' AND expiresAt <= :nowMillis")
    suspend fun expireOldConfirmations(nowMillis: Long = System.currentTimeMillis()): Int

    // --- Atomic Transactions ---

    @Transaction
    suspend fun recordAppUsageCompletion(
        appId: Long,
        date: String,
        timestamp: Long,
        method: String = "AUTOMATIC_USAGE"
    ): Boolean {
        val existing = getAppCompletionForDate(appId, date)
        if (existing != null) {
            return false // Already recorded today
        }
        insertCompletion(
            AppStreakCompletionEntity(
                trackedAppId = appId,
                accountId = null,
                completionDate = date,
                detectedAt = timestamp,
                completionMethod = method,
                confirmationStatus = "CONFIRMED"
            )
        )
        val app = getTrackedAppById(appId)
        if (app != null) {
            updateTrackedApp(app.copy(lastCompletedDate = date, updatedAt = System.currentTimeMillis()))
        }
        return true
    }

    @Transaction
    suspend fun recordAccountCompletion(
        appId: Long,
        accountId: Long,
        date: String,
        timestamp: Long,
        method: String = "MANUAL_CONFIRMATION"
    ): Boolean {
        val existing = getAccountCompletionForDate(accountId, date)
        if (existing != null) {
            return false // Already recorded today
        }
        insertCompletion(
            AppStreakCompletionEntity(
                trackedAppId = appId,
                accountId = accountId,
                completionDate = date,
                detectedAt = timestamp,
                completionMethod = method,
                confirmationStatus = "CONFIRMED"
            )
        )
        val account = getAccountById(accountId)
        if (account != null) {
            updateAccount(account.copy(lastCompletedDate = date, updatedAt = System.currentTimeMillis()))
        }
        return true
    }
}
