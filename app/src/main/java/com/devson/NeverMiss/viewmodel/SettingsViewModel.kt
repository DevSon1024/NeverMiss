package com.devson.NeverMiss.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.data.local.db.CompletionHistoryEntity
import com.devson.NeverMiss.data.local.db.NeverMissDatabase
import com.devson.NeverMiss.data.local.db.StreakEntity
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.core.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsState(
    val theme: UserPreferences.AppTheme = UserPreferences.AppTheme.SYSTEM,
    val dynamicColorsEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 15,
    val appLockPin: String = "",
    val devOverrideDateEnabled: Boolean = false,
    val devOverrideDate: String = "",
    val backupSuccess: Boolean? = null,
    val restoreSuccess: Boolean? = null,
    val errorMessage: String? = null
)

@Serializable
data class BackupData(
    val streaks: List<StreakBackup>,
    val freezesBalance: Int = 0
)

@Serializable
data class StreakBackup(
    val title: String,
    val description: String,
    val emoji: String,
    val categoryName: String,
    val color: Int,
    val creationDate: String,
    val isArchived: Boolean,
    val isPaused: Boolean,
    val repeatIntervalDays: Int,
    val daysOfWeek: String,
    val reminderTime: String,
    val reminderRepeatHours: Int,
    val remindUntilDone: Boolean,
    val dangerReminderMinutes: Int,
    val cutoffTime: String,
    val gracePeriodUntil3AM: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastCompletedDate: String,
    val notificationSound: String?,
    val vibrate: Boolean,
    val silent: Boolean,
    val persistent: Boolean,
    val history: List<HistoryBackup>
)

@Serializable
data class HistoryBackup(
    val date: String,
    val status: String,
    val timestamp: Long
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val database: NeverMissDatabase,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _operationStatus = MutableStateFlow<Pair<Boolean?, Boolean?>>(null to null) // backupSuccess, restoreSuccess
    private val _errorStatus = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsState> = combine(
        userPreferences.themeFlow,
        userPreferences.dynamicColorsFlow,
        userPreferences.snoozeDurationMinutesFlow,
        userPreferences.appLockPinFlow,
        userPreferences.devOverrideDateEnabledFlow,
        userPreferences.devOverrideDateFlow,
        _operationStatus,
        _errorStatus
    ) { flowsArray ->
        val theme = flowsArray[0] as UserPreferences.AppTheme
        val dynamicColors = flowsArray[1] as Boolean
        val snooze = flowsArray[2] as Int
        val pin = flowsArray[3] as String
        val devOverrideEnabled = flowsArray[4] as Boolean
        val devOverrideDate = flowsArray[5] as String
        @Suppress("UNCHECKED_CAST")
        val status = flowsArray[6] as Pair<Boolean?, Boolean?>
        val err = flowsArray[7] as String?

        SettingsState(
            theme = theme,
            dynamicColorsEnabled = dynamicColors,
            snoozeDurationMinutes = snooze,
            appLockPin = pin,
            devOverrideDateEnabled = devOverrideEnabled,
            devOverrideDate = devOverrideDate,
            backupSuccess = status.first,
            restoreSuccess = status.second,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun setTheme(theme: UserPreferences.AppTheme) {
        viewModelScope.launch {
            userPreferences.setTheme(theme)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDynamicColors(enabled)
        }
    }

    fun setSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setSnoozeDuration(minutes)
        }
    }

    fun setAppLockPin(pin: String) {
        viewModelScope.launch {
            userPreferences.setAppLockPin(pin)
        }
    }

    fun setDevOverrideDateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDevOverrideDateEnabled(enabled)
        }
    }

    fun setDevOverrideDate(dateStr: String) {
        viewModelScope.launch {
            userPreferences.setDevOverrideDate(dateStr)
        }
    }

    fun exportBackup(backupFile: File) {
        viewModelScope.launch {
            try {
                _operationStatus.value = null to null
                _errorStatus.value = null

                val dao = database.streakDao()
                val allStreakEntities = database.streakDao().getAllActiveStreaks() // forces load
                val allStreaks = dao.getAllStreaksFlow().first()
                
                val streaksBackup = mutableListOf<StreakBackup>()

                for (entity in allStreaks) {
                    val historyEntities = dao.getHistoryForStreak(entity.id)
                    val historyBackup = historyEntities.map { hist ->
                        HistoryBackup(
                            date = hist.date,
                            status = hist.status,
                            timestamp = hist.timestamp
                        )
                    }

                    streaksBackup.add(
                        StreakBackup(
                            title = entity.title,
                            description = entity.description,
                            emoji = entity.emoji,
                            categoryName = entity.category,
                            color = entity.color,
                            creationDate = entity.creationDate,
                            isArchived = entity.isArchived,
                            isPaused = entity.isPaused,
                            repeatIntervalDays = entity.repeatIntervalDays,
                            daysOfWeek = entity.daysOfWeek,
                            reminderTime = entity.reminderTime,
                            reminderRepeatHours = entity.reminderRepeatHours,
                            remindUntilDone = entity.remindUntilDone,
                            dangerReminderMinutes = entity.dangerReminderMinutes,
                            cutoffTime = entity.cutoffTime,
                            gracePeriodUntil3AM = entity.gracePeriodUntil3AM,
                            currentStreak = entity.currentStreak,
                            longestStreak = entity.longestStreak,
                            lastCompletedDate = entity.lastCompletedDate,
                            notificationSound = entity.notificationSound,
                            vibrate = entity.vibrate,
                            silent = entity.silent,
                            persistent = entity.persistent,
                            history = historyBackup
                        )
                    )
                }

                val backupData = BackupData(
                    streaks = streaksBackup
                )

                val jsonString = json.encodeToString(BackupData.serializer(), backupData)
                backupFile.writeText(jsonString)

                _operationStatus.value = true to null
            } catch (e: Exception) {
                _errorStatus.value = "Export failed: ${e.localizedMessage}"
                _operationStatus.value = false to null
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                _operationStatus.value = null to null
                _errorStatus.value = null

                val backupData = json.decodeFromString(BackupData.serializer(), jsonString)
                val dao = database.streakDao()

                // Cancel existing alarms before clearing database
                val currentStreaks = dao.getAllActiveStreaks()
                currentStreaks.forEach {
                    reminderScheduler.cancelAlarm(it.id)
                }

                // Clear Database
                database.clearAllTables()

                // Restore Streaks and Nested History
                backupData.streaks.forEach { backupStreak ->
                    val entity = StreakEntity(
                        title = backupStreak.title,
                        description = backupStreak.description,
                        emoji = backupStreak.emoji,
                        category = backupStreak.categoryName,
                        color = backupStreak.color,
                        creationDate = backupStreak.creationDate,
                        isArchived = backupStreak.isArchived,
                        isPaused = backupStreak.isPaused,
                        repeatIntervalDays = backupStreak.repeatIntervalDays,
                        daysOfWeek = backupStreak.daysOfWeek,
                        reminderTime = backupStreak.reminderTime,
                        reminderRepeatHours = backupStreak.reminderRepeatHours,
                        remindUntilDone = backupStreak.remindUntilDone,
                        dangerReminderMinutes = backupStreak.dangerReminderMinutes,
                        cutoffTime = backupStreak.cutoffTime,
                        gracePeriodUntil3AM = backupStreak.gracePeriodUntil3AM,
                        currentStreak = backupStreak.currentStreak,
                        longestStreak = backupStreak.longestStreak,
                        lastCompletedDate = backupStreak.lastCompletedDate,
                        notificationSound = backupStreak.notificationSound,
                        vibrate = backupStreak.vibrate,
                        silent = backupStreak.silent,
                        persistent = backupStreak.persistent
                    )
                    
                    val newStreakId = dao.insertStreak(entity)

                    // Restore nested history records
                    backupStreak.history.forEach { historyBackup ->
                        val historyEntity = CompletionHistoryEntity(
                            streakId = newStreakId,
                            date = historyBackup.date,
                            status = historyBackup.status,
                            timestamp = historyBackup.timestamp
                        )
                        dao.insertHistoryEntry(historyEntity)
                    }

                    // Reschedule alarm for this restored streak if active
                    if (!entity.isArchived && !entity.isPaused) {
                        reminderScheduler.scheduleNextAlarm(newStreakId)
                    }
                }

                _operationStatus.value = null to true
            } catch (e: Exception) {
                _errorStatus.value = "Import failed: ${e.localizedMessage}"
                _operationStatus.value = null to false
            }
        }
    }

    fun clearStatus() {
        _operationStatus.value = null to null
        _errorStatus.value = null
    }
}
