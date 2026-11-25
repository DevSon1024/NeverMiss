package com.devson.NeverMiss.core.reminder.interval

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class AlertSoundOption(val id: String, val title: String) {
    DEFAULT_ALARM("default_alarm", "Default Alarm Sound"),
    DEFAULT_NOTIFICATION("default_notification", "Default Notification Sound"),
    SILENT("silent", "Silent (No Sound)")
}

enum class VibrationPatternOption(val id: String, val title: String, val timings: LongArray) {
    SHORT("short", "Short (Quick pulse)", longArrayOf(0, 250, 200, 250)),
    STANDARD("standard", "Standard", longArrayOf(0, 500, 500, 500)),
    LONG("long", "Long (Strong alert)", longArrayOf(0, 1000, 500, 1000))
}

data class IntervalTimerSettings(
    val soundOption: AlertSoundOption = AlertSoundOption.DEFAULT_ALARM,
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: VibrationPatternOption = VibrationPatternOption.STANDARD,
    val alertDurationSeconds: Int = 30, // 10, 30, 60, 120
    val snoozeDurationMinutes: Int = 5 // 1, 5, 10, 15, 30
)

private val Context.timerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "interval_timer_settings")

@Singleton
class IntervalTimerSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.timerSettingsDataStore

    private object Keys {
        val SOUND_OPTION = stringPreferencesKey("timer_sound_option")
        val VIBRATION_ENABLED = booleanPreferencesKey("timer_vibration_enabled")
        val VIBRATION_PATTERN = stringPreferencesKey("timer_vibration_pattern")
        val ALERT_DURATION_SECONDS = intPreferencesKey("timer_alert_duration_seconds")
        val SNOOZE_DURATION_MINUTES = intPreferencesKey("timer_snooze_duration_minutes")
    }

    val settingsFlow: Flow<IntervalTimerSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            val soundId = prefs[Keys.SOUND_OPTION] ?: AlertSoundOption.DEFAULT_ALARM.id
            val soundOpt = AlertSoundOption.values().find { it.id == soundId } ?: AlertSoundOption.DEFAULT_ALARM

            val vibEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true

            val vibPatternId = prefs[Keys.VIBRATION_PATTERN] ?: VibrationPatternOption.STANDARD.id
            val vibPatternOpt = VibrationPatternOption.values().find { it.id == vibPatternId } ?: VibrationPatternOption.STANDARD

            val alertDuration = prefs[Keys.ALERT_DURATION_SECONDS] ?: 30
            val snoozeDuration = prefs[Keys.SNOOZE_DURATION_MINUTES] ?: 5

            IntervalTimerSettings(
                soundOption = soundOpt,
                vibrationEnabled = vibEnabled,
                vibrationPattern = vibPatternOpt,
                alertDurationSeconds = alertDuration.coerceIn(5, 300),
                snoozeDurationMinutes = snoozeDuration.coerceIn(1, 60)
            )
        }

    suspend fun updateSoundOption(option: AlertSoundOption) {
        dataStore.edit { it[Keys.SOUND_OPTION] = option.id }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateVibrationPattern(pattern: VibrationPatternOption) {
        dataStore.edit { it[Keys.VIBRATION_PATTERN] = pattern.id }
    }

    suspend fun updateAlertDurationSeconds(seconds: Int) {
        dataStore.edit { it[Keys.ALERT_DURATION_SECONDS] = seconds.coerceIn(5, 300) }
    }

    suspend fun updateSnoozeDurationMinutes(minutes: Int) {
        dataStore.edit { it[Keys.SNOOZE_DURATION_MINUTES] = minutes.coerceIn(1, 60) }
    }
}
