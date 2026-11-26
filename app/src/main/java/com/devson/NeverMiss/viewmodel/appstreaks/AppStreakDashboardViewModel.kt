package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import com.devson.NeverMiss.domain.usecase.appstreaks.RemoveTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ToggleTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.UpdateTrackedAppMetadataUseCase
import com.devson.NeverMiss.domain.usecase.common.DeleteStreakWithConfirmationUseCase
import com.devson.NeverMiss.domain.usecase.common.DeletionConfirmation
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakArchiveUseCase
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakPauseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
data class DashboardActionState(
    val showDeleteDialog: Boolean = false,
    val deleteTargetType: StreakType? = null,
    val deleteTargetId: Long = -1L,
    val deletionConfirmation: DeletionConfirmation? = null,
    val actionError: String? = null
)

@Immutable
data class AppStreakDashboardUiState(
    val isLoading: Boolean = true,
    val trackedApps: List<TrackedAppSummary> = emptyList(),
    val totalTracked: Int = 0,
    val completedTodayCount: Int = 0,
    val highestStreak: Int = 0,
    val normalStreaks: List<Streak> = emptyList(),
    val normalCompletedTodayCount: Int = 0,
    val normalHighestStreak: Int = 0,
    val actionState: DashboardActionState = DashboardActionState()
)

@HiltViewModel
class AppStreakDashboardViewModel @Inject constructor(
    private val appStreakRepository: AppStreakRepository,
    private val streakRepository: StreakRepository,
    private val toggleTrackedAppUseCase: ToggleTrackedAppUseCase,
    private val removeTrackedAppUseCase: RemoveTrackedAppUseCase,
    private val toggleStreakPauseUseCase: ToggleStreakPauseUseCase,
    private val toggleStreakArchiveUseCase: ToggleStreakArchiveUseCase,
    private val deleteStreakWithConfirmationUseCase: DeleteStreakWithConfirmationUseCase,
    private val updateTrackedAppMetadataUseCase: UpdateTrackedAppMetadataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppStreakDashboardUiState())
    val uiState: StateFlow<AppStreakDashboardUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow(DashboardActionState())

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                appStreakRepository.getAllTrackedAppsFlow(),
                streakRepository.getActiveStreaksFlow(),
                _actionState
            ) { apps, normalList, actionState ->
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
                val activeNormal = normalList.filter { !it.isArchived }
                val normalDoneToday = activeNormal.count { it.lastCompletedDate == todayStr }
                val normalMax = activeNormal.maxOfOrNull { it.currentStreak } ?: 0

                AppStreakDashboardUiState(
                    isLoading = false,
                    trackedApps = summaries,
                    totalTracked = apps.size,
                    completedTodayCount = completedToday,
                    highestStreak = maxStreak,
                    normalStreaks = activeNormal,
                    normalCompletedTodayCount = normalDoneToday,
                    normalHighestStreak = normalMax,
                    actionState = actionState
                )
            }.collect { newState ->
                _uiState.value = newState
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

    fun togglePause(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                toggleStreakPauseUseCase(type, id)
            } catch (e: Exception) {
                _actionState.update { it.copy(actionError = e.localizedMessage ?: "Failed to toggle pause") }
            }
        }
    }

    fun toggleArchive(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                toggleStreakArchiveUseCase(type, id)
            } catch (e: Exception) {
                _actionState.update { it.copy(actionError = e.localizedMessage ?: "Failed to toggle archive") }
            }
        }
    }

    fun requestDelete(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                val confirmation = deleteStreakWithConfirmationUseCase.getConfirmationInfo(type, id)
                _actionState.update {
                    it.copy(
                        showDeleteDialog = true,
                        deleteTargetType = type,
                        deleteTargetId = id,
                        deletionConfirmation = confirmation
                    )
                }
            } catch (e: Exception) {
                _actionState.update { it.copy(actionError = e.localizedMessage ?: "Failed to prepare deletion") }
            }
        }
    }

    fun dismissDeleteDialog() {
        _actionState.update {
            it.copy(
                showDeleteDialog = false,
                deleteTargetType = null,
                deleteTargetId = -1L,
                deletionConfirmation = null
            )
        }
    }

    fun confirmDelete() {
        val current = _actionState.value
        val type = current.deleteTargetType ?: return
        val id = current.deleteTargetId
        if (id < 0L) return

        viewModelScope.launch {
            try {
                val success = deleteStreakWithConfirmationUseCase.executeDelete(type, id)
                _actionState.update {
                    it.copy(
                        showDeleteDialog = false,
                        deleteTargetType = null,
                        deleteTargetId = -1L,
                        deletionConfirmation = null,
                        actionError = if (success) null else "Could not delete streak"
                    )
                }
            } catch (e: Exception) {
                _actionState.update {
                    it.copy(
                        showDeleteDialog = false,
                        actionError = e.localizedMessage ?: "Deletion failed"
                    )
                }
            }
        }
    }

    fun updateAppName(appId: Long, newName: String) {
        viewModelScope.launch {
            try {
                updateTrackedAppMetadataUseCase.updateAppName(appId, newName)
            } catch (e: Exception) {
                _actionState.update { it.copy(actionError = e.localizedMessage ?: "Failed to update app name") }
            }
        }
    }

    fun clearActionError() {
        _actionState.update { it.copy(actionError = null) }
    }
}

