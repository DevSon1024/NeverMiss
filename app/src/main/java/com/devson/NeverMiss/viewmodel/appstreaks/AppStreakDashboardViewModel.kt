package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.usecase.appstreaks.RemoveTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ToggleTrackedAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TrackedAppSummary(
    val app: TrackedApp,
    val isCompletedToday: Boolean,
    val accountsCount: Int
)

@Immutable
data class AppStreakDashboardUiState(
    val isLoading: Boolean = true,
    val trackedApps: List<TrackedAppSummary> = emptyList(),
    val totalTracked: Int = 0,
    val completedTodayCount: Int = 0,
    val highestStreak: Int = 0
)

@HiltViewModel
class AppStreakDashboardViewModel @Inject constructor(
    private val appStreakRepository: AppStreakRepository,
    private val toggleTrackedAppUseCase: ToggleTrackedAppUseCase,
    private val removeTrackedAppUseCase: RemoveTrackedAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppStreakDashboardUiState())
    val uiState: StateFlow<AppStreakDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            appStreakRepository.getAllTrackedAppsFlow().collect { apps ->
                val todayStr = StreakCalculator.formatDate(AppClock.now())
                val summaries = apps.map { app ->
                    val isCompleted = appStreakRepository.isAppCompletedOnDate(app.id, todayStr)
                    val accounts = appStreakRepository.getAccountsForApp(app.id)
                    TrackedAppSummary(
                        app = app,
                        isCompletedToday = isCompleted,
                        accountsCount = accounts.size
                    )
                }

                val completedToday = summaries.count { it.isCompletedToday }
                val maxStreak = apps.maxOfOrNull { it.currentStreak } ?: 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trackedApps = summaries,
                        totalTracked = apps.size,
                        completedTodayCount = completedToday,
                        highestStreak = maxStreak
                    )
                }
            }
        }
    }

    fun toggleAppTracking(appId: Long) {
        viewModelScope.launch {
            toggleTrackedAppUseCase(appId)
        }
    }

    fun removeApp(appId: Long) {
        viewModelScope.launch {
            removeTrackedAppUseCase(appId)
        }
    }
}

