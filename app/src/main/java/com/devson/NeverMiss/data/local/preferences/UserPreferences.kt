package com.devson.NeverMiss.data.local.preferences

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nevermiss_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val themeFlow: Flow<AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }

    val dynamicColorsFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLORS] ?: true
        }

    val snoozeDurationMinutesFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION] ?: 15
        }

    val appLockPinFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN] ?: ""
        }

    val devOverrideDateEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DEV_OVERRIDE_DATE_ENABLED] ?: false
        }

    val devOverrideDateFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DEV_OVERRIDE_DATE] ?: ""
        }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setSnoozeDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION] = minutes
        }
    }

    suspend fun setAppLockPin(pin: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    suspend fun setDevOverrideDateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEV_OVERRIDE_DATE_ENABLED] = enabled
        }
    }

    suspend fun setDevOverrideDate(dateStr: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEV_OVERRIDE_DATE] = dateStr
        }
    }

    enum class AppTheme {
        LIGHT, DARK, SYSTEM
    }

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val DEV_OVERRIDE_DATE_ENABLED = booleanPreferencesKey("dev_override_date_enabled")
        val DEV_OVERRIDE_DATE = stringPreferencesKey("dev_override_date")
    }
}
