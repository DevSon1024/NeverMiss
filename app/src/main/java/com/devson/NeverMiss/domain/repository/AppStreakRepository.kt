package com.devson.NeverMiss.domain.repository

import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint
import kotlinx.coroutines.flow.Flow

interface AppStreakRepository {

    // --- Tracked Apps ---
    fun getAllTrackedAppsFlow(): Flow<List<TrackedApp>>
    fun getActiveTrackedAppsFlow(): Flow<List<TrackedApp>>
    fun getArchivedTrackedAppsFlow(): Flow<List<TrackedApp>>
    suspend fun getAllTrackedApps(): List<TrackedApp>
    suspend fun getEnabledTrackedApps(): List<TrackedApp>
    suspend fun getTrackedAppById(id: Long): TrackedApp?
    fun getTrackedAppByIdFlow(id: Long): Flow<TrackedApp?>
    suspend fun getTrackedAppByPackage(packageName: String): TrackedApp?
    suspend fun addTrackedApp(app: TrackedApp): Long
    suspend fun updateTrackedApp(app: TrackedApp)
    suspend fun deleteTrackedApp(app: TrackedApp)
    suspend fun deleteTrackedAppById(id: Long)

    // --- Tracked Accounts ---
    fun getAllAccountsFlow(): Flow<List<TrackedAccount>>
    fun getAccountsForAppFlow(appId: Long): Flow<List<TrackedAccount>>
    suspend fun getAccountsForApp(appId: Long): List<TrackedAccount>
    suspend fun getEnabledAccountsForApp(appId: Long): List<TrackedAccount>
    suspend fun getAccountById(id: Long): TrackedAccount?
    fun getAccountByIdFlow(id: Long): Flow<TrackedAccount?>
    suspend fun addAccount(account: TrackedAccount): Long
    suspend fun updateAccount(account: TrackedAccount)
    suspend fun deleteAccount(account: TrackedAccount)

    // --- Completions ---
    fun getCompletionsForAppFlow(appId: Long): Flow<List<AppStreakCompletion>>
    suspend fun getCompletionsForApp(appId: Long): List<AppStreakCompletion>
    fun getCompletionsForAccountFlow(accountId: Long): Flow<List<AppStreakCompletion>>
    suspend fun getCompletionsForAccount(accountId: Long): List<AppStreakCompletion>
    suspend fun isAppCompletedOnDate(appId: Long, date: String): Boolean
    suspend fun isAccountCompletedOnDate(accountId: Long, date: String): Boolean
    suspend fun logAppCompletion(
        appId: Long,
        date: String,
        timestamp: Long = System.currentTimeMillis(),
        method: CompletionMethod = CompletionMethod.AUTOMATIC_USAGE
    ): Boolean
    suspend fun logAccountCompletion(
        appId: Long,
        accountId: Long,
        date: String,
        timestamp: Long = System.currentTimeMillis(),
        method: CompletionMethod = CompletionMethod.MANUAL_CONFIRMATION
    ): Boolean
    suspend fun undoAccountCompletion(appId: Long, accountId: Long, date: String)

    // --- Usage Checkpoints ---
    suspend fun getCheckpoint(source: String = UsageCheckpoint.DEFAULT_SOURCE): UsageCheckpoint?
    suspend fun saveCheckpoint(source: String = UsageCheckpoint.DEFAULT_SOURCE, timestamp: Long)

    // --- Pending Account Confirmations ---
    fun getActivePendingConfirmationsFlow(nowMillis: Long = System.currentTimeMillis()): Flow<List<PendingConfirmation>>
    suspend fun getPendingConfirmationsForApp(appId: Long, nowMillis: Long = System.currentTimeMillis()): List<PendingConfirmation>
    suspend fun getPendingConfirmationById(id: Long): PendingConfirmation?
    suspend fun addPendingConfirmation(confirmation: PendingConfirmation): Long
    suspend fun updatePendingConfirmation(confirmation: PendingConfirmation)
    suspend fun expireOldConfirmations(nowMillis: Long = System.currentTimeMillis()): Int
}
