package com.devson.NeverMiss.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import com.devson.NeverMiss.domain.usecase.appstreaks.UpdateTrackedAppMetadataUseCase
import com.devson.NeverMiss.domain.usecase.common.DeleteStreakWithConfirmationUseCase
import com.devson.NeverMiss.domain.usecase.common.DeletionConfirmation
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakArchiveUseCase
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakPauseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CardCompletionStatus {
    NONE,
    PARTIAL,
    ALL_DONE,
    PAUSED
}

data class HomeAppStreakItem(
    val app: TrackedApp,
    val accountCount: Int,
    val completedAccountCount: Int,
    val displayStreak: Int,
    val accountSummary: String,
    val isCompletedToday: Boolean,
    val completionStatus: CardCompletionStatus
)

@Immutable
data class HomeActionDialogState(
    val showDeleteDialog: Boolean = false,
    val deleteTargetType: StreakType? = null,
    val deleteTargetId: Long = -1L,
    val deletionConfirmation: DeletionConfirmation? = null,
    val actionError: String? = null
)

@Immutable
data class HomeState(
    val normalStreaks: List<Streak> = emptyList(),
    val appStreaks: List<HomeAppStreakItem> = emptyList(),
    val searchQuery: String = "",
    val isGridLayout: Boolean = false,
    val isLoading: Boolean = false,
    val actionDialogState: HomeActionDialogState = HomeActionDialogState()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository,
    private val userPreferences: UserPreferences,
    private val toggleStreakPauseUseCase: ToggleStreakPauseUseCase,
    private val toggleStreakArchiveUseCase: ToggleStreakArchiveUseCase,
    private val deleteStreakWithConfirmationUseCase: DeleteStreakWithConfirmationUseCase,
    private val updateTrackedAppMetadataUseCase: UpdateTrackedAppMetadataUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _actionDialogState = MutableStateFlow(HomeActionDialogState())

    val state: StateFlow<HomeState> = combine(
        streakRepository.getActiveStreaksFlow(),
        appStreakRepository.getActiveTrackedAppsFlow(),
        appStreakRepository.getAllAccountsFlow(),
        _searchQuery,
        userPreferences.homeGridLayoutFlow,
        _actionDialogState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val activeStreaks = values[0] as List<Streak>
        @Suppress("UNCHECKED_CAST")
        val activeApps = values[1] as List<TrackedApp>
        @Suppress("UNCHECKED_CAST")
        val allAccounts = values[2] as List<com.devson.NeverMiss.domain.model.apptracking.TrackedAccount>
        val search = values[3] as String
        val isGridLayout = values[4] as Boolean
        val actionDialogState = values[5] as HomeActionDialogState

        val todayStr = StreakCalculator.formatDate(AppClock.now())
        val trimmedQuery = search.trim()

        val filteredNormal = activeStreaks.filter { streak ->
            !streak.isArchived && (
                trimmedQuery.isEmpty() ||
                streak.title.contains(trimmedQuery, ignoreCase = true) ||
                streak.description.contains(trimmedQuery, ignoreCase = true)
            )
        }

        val accountsByApp = allAccounts.groupBy { it.trackedAppId }
        val filteredApps = activeApps.filter { app ->
            !app.isArchived && (
                trimmedQuery.isEmpty() ||
                app.appName.contains(trimmedQuery, ignoreCase = true) ||
                app.packageName.contains(trimmedQuery, ignoreCase = true)
            )
        }.map { app ->
            val appAccounts = accountsByApp[app.id] ?: emptyList()
            val activeAccounts = appAccounts.filter { it.isEnabled && !it.isArchived }
            val completedAccounts = activeAccounts.count { it.lastCompletedDate == todayStr }
            val isCompletedToday = when {
                activeAccounts.size > 1 -> completedAccounts >= activeAccounts.size
                activeAccounts.size == 1 -> activeAccounts.first().lastCompletedDate == todayStr || app.lastCompletedDate == todayStr
                else -> app.lastCompletedDate == todayStr
            }
            val completionStatus = when {
                app.isPaused -> CardCompletionStatus.PAUSED
                activeAccounts.size > 1 -> when {
                    completedAccounts >= activeAccounts.size -> CardCompletionStatus.ALL_DONE
                    completedAccounts > 0 -> CardCompletionStatus.PARTIAL
                    else -> CardCompletionStatus.NONE
                }
                isCompletedToday -> CardCompletionStatus.ALL_DONE
                else -> CardCompletionStatus.NONE
            }
            val displayStreak = if (activeAccounts.isNotEmpty()) {
                activeAccounts.maxOf { it.currentStreak }
            } else {
                app.currentStreak
            }
            val accountSummary = when {
                appAccounts.isEmpty() -> "No accounts configured"
                appAccounts.size == 1 -> appAccounts.first().accountName
                else -> "${appAccounts.size} accounts (${appAccounts.joinToString(", ") { it.accountName }})"
            }
            HomeAppStreakItem(
                app = app,
                accountCount = appAccounts.size,
                completedAccountCount = completedAccounts,
                displayStreak = displayStreak,
                accountSummary = accountSummary,
                isCompletedToday = isCompletedToday,
                completionStatus = completionStatus
            )
        }

        HomeState(
            normalStreaks = filteredNormal,
            appStreaks = filteredApps,
            searchQuery = search,
            isGridLayout = isGridLayout,
            isLoading = false,
            actionDialogState = actionDialogState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridLayout() {
        val next = !state.value.isGridLayout
        viewModelScope.launch {
            userPreferences.setHomeGridLayout(next)
        }
    }

    fun togglePause(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                toggleStreakPauseUseCase(type, id)
            } catch (e: Exception) {
                _actionDialogState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to toggle pause")
                }
            }
        }
    }

    fun toggleArchive(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                toggleStreakArchiveUseCase(type, id)
            } catch (e: Exception) {
                _actionDialogState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to toggle archive")
                }
            }
        }
    }

    fun requestDelete(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                val confirmation = deleteStreakWithConfirmationUseCase.getConfirmationInfo(type, id)
                _actionDialogState.update {
                    it.copy(
                        showDeleteDialog = true,
                        deleteTargetType = type,
                        deleteTargetId = id,
                        deletionConfirmation = confirmation
                    )
                }
            } catch (e: Exception) {
                _actionDialogState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to prepare deletion")
                }
            }
        }
    }

    fun dismissDeleteDialog() {
        _actionDialogState.update {
            it.copy(
                showDeleteDialog = false,
                deleteTargetType = null,
                deleteTargetId = -1L,
                deletionConfirmation = null
            )
        }
    }

    fun confirmDelete() {
        val current = _actionDialogState.value
        val type = current.deleteTargetType ?: return
        val id = current.deleteTargetId
        if (id < 0L) return

        viewModelScope.launch {
            try {
                val success = deleteStreakWithConfirmationUseCase.executeDelete(type, id)
                _actionDialogState.update {
                    it.copy(
                        showDeleteDialog = false,
                        deleteTargetType = null,
                        deleteTargetId = -1L,
                        deletionConfirmation = null,
                        actionError = if (success) null else "Could not delete streak"
                    )
                }
            } catch (e: Exception) {
                _actionDialogState.update {
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
                _actionDialogState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to update app name")
                }
            }
        }
    }

    fun clearActionError() {
        _actionDialogState.update { it.copy(actionError = null) }
    }
}
