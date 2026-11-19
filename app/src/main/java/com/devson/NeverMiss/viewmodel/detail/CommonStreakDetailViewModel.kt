package com.devson.NeverMiss.viewmodel.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import com.devson.NeverMiss.domain.usecase.LogCompletionUseCase
import com.devson.NeverMiss.domain.usecase.common.DeleteStreakWithConfirmationUseCase
import com.devson.NeverMiss.domain.usecase.common.DeletionConfirmation
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakArchiveUseCase
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakPauseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Immutable
data class CommonStreakDetailState(
    val detail: SharedStreakDetail? = null,
    val completedDates: Set<String> = emptySet(),
    val missedDates: Set<String> = emptySet(),
    val currentMonth: YearMonth = YearMonth.now(),
    val accounts: List<TrackedAccount> = emptyList(),
    val isCompletedToday: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val deletionConfirmation: DeletionConfirmation? = null,
    val showDeleteDialog: Boolean = false,
    val actionError: String? = null
)

@HiltViewModel
class CommonStreakDetailViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository,
    private val logCompletionUseCase: LogCompletionUseCase,
    private val toggleStreakPauseUseCase: ToggleStreakPauseUseCase,
    private val toggleStreakArchiveUseCase: ToggleStreakArchiveUseCase,
    private val deleteStreakWithConfirmationUseCase: DeleteStreakWithConfirmationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommonStreakDetailState())
    val uiState: StateFlow<CommonStreakDetailState> = _uiState.asStateFlow()

    private var currentType: StreakType? = null
    private var currentId: Long = -1L

    fun load(type: StreakType, id: Long) {
        if (currentType == type && currentId == id && _uiState.value.detail != null) return
        currentType = type
        currentId = id

        _uiState.update { it.copy(isLoading = true, actionError = null) }

        when (type) {
            StreakType.NORMAL -> observeNormalStreak(id)
            StreakType.APP -> observeAppStreak(id)
        }
    }

    private fun observeNormalStreak(id: Long) {
        viewModelScope.launch {
            combine(
                streakRepository.getStreakByIdFlow(id),
                streakRepository.getHistoryForStreakFlow(id)
            ) { streak, history ->
                if (streak == null) {
                    _uiState.update { it.copy(detail = null, isLoading = false) }
                } else {
                    val today = AppClock.now()
                    val todayStr = StreakCalculator.formatDate(today)
                    val completedDates = history
                        .filter { it.status == CompletionStatus.COMPLETED }
                        .map { it.date }
                        .toSet()
                    val isCompletedToday = completedDates.contains(todayStr) || streak.lastCompletedDate == todayStr

                    val startDateStr = streak.startDate.ifEmpty { streak.creationDate }
                    val startLocalDate = try {
                        StreakCalculator.parseDate(startDateStr)
                    } catch (e: Exception) {
                        today
                    }

                    val missedDates = mutableSetOf<String>()
                    val historyMap = history.associateBy { it.date }
                    if (!startLocalDate.isAfter(today.minusDays(1))) {
                        val totalDays = ChronoUnit.DAYS.between(startLocalDate, today.minusDays(1))
                        for (i in 0..totalDays) {
                            val date = startLocalDate.plusDays(i)
                            val dateStr = StreakCalculator.formatDate(date)
                            if (StreakCalculator.isScheduled(streak, date) && !completedDates.contains(dateStr)) {
                                val status = historyMap[dateStr]?.status
                                if (status != CompletionStatus.PAUSED && status != CompletionStatus.SKIPPED) {
                                    missedDates.add(dateStr)
                                }
                            }
                        }
                    }

                    val sharedDetail = SharedStreakDetail(
                        id = streak.id,
                        type = StreakType.NORMAL,
                        name = streak.title,
                        description = streak.description.ifEmpty { null },
                        icon = streak.icon,
                        emoji = streak.emoji,
                        color = streak.color,
                        currentStreak = streak.currentStreak,
                        longestStreak = streak.longestStreak,
                        lastCompletedDate = streak.lastCompletedDate,
                        isPaused = streak.isPaused,
                        isArchived = streak.isArchived,
                        status = streak.status,
                        creationDate = streak.creationDate,
                        startDate = streak.startDate.ifEmpty { streak.creationDate },
                        totalCompletedDays = completedDates.size,
                        updatedAt = streak.updatedAt
                    )

                    _uiState.update {
                        it.copy(
                            detail = sharedDetail,
                            completedDates = completedDates,
                            missedDates = missedDates,
                            isCompletedToday = isCompletedToday,
                            isLoading = false
                        )
                    }
                }
            }.collect()
        }
    }

    private fun observeAppStreak(id: Long) {
        viewModelScope.launch {
            combine(
                appStreakRepository.getTrackedAppByIdFlow(id),
                appStreakRepository.getAccountsForAppFlow(id),
                appStreakRepository.getCompletionsForAppFlow(id)
            ) { app, accounts, completions ->
                if (app == null) {
                    _uiState.update { it.copy(detail = null, isLoading = false) }
                } else {
                    val today = AppClock.now()
                    val todayStr = StreakCalculator.formatDate(today)
                    val completedDates = completions.map { it.completionDate }.toSet()
                    val isCompletedToday = app.lastCompletedDate == todayStr

                    val creationDateStr = StreakCalculator.formatDate(
                        Instant.ofEpochMilli(app.trackingCreatedAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )

                    val sharedDetail = SharedStreakDetail(
                        id = app.id,
                        type = StreakType.APP,
                        name = app.appName,
                        description = app.packageName,
                        icon = null,
                        emoji = null,
                        color = null,
                        currentStreak = app.currentStreak,
                        longestStreak = app.longestStreak,
                        lastCompletedDate = app.lastCompletedDate,
                        isPaused = app.isPaused,
                        isArchived = app.isArchived,
                        status = app.status,
                        creationDate = creationDateStr,
                        startDate = creationDateStr,
                        totalCompletedDays = completedDates.size,
                        updatedAt = app.updatedAt
                    )

                    _uiState.update {
                        it.copy(
                            detail = sharedDetail,
                            completedDates = completedDates,
                            missedDates = emptySet(),
                            accounts = accounts,
                            isCompletedToday = isCompletedToday,
                            isLoading = false
                        )
                    }
                }
            }.collect()
        }
    }

    fun completeToday() {
        val current = _uiState.value.detail ?: return
        if (current.type != StreakType.NORMAL) return
        if (current.isPaused || current.isArchived) return
        if (_uiState.value.isCompletedToday) return

        viewModelScope.launch {
            try {
                val todayStr = StreakCalculator.formatDate(AppClock.now())
                logCompletionUseCase(current.id, todayStr, CompletionStatus.COMPLETED)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to log completion")
                }
            }
        }
    }

    fun togglePause() {
        val current = _uiState.value.detail ?: return
        viewModelScope.launch {
            try {
                toggleStreakPauseUseCase(current.type, current.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to update pause state")
                }
            }
        }
    }

    fun toggleArchive() {
        val current = _uiState.value.detail ?: return
        viewModelScope.launch {
            try {
                toggleStreakArchiveUseCase(current.type, current.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to update archive state")
                }
            }
        }
    }

    fun requestDelete() {
        val current = _uiState.value.detail ?: return
        viewModelScope.launch {
            try {
                val info = deleteStreakWithConfirmationUseCase.getConfirmationInfo(current.type, current.id)
                _uiState.update { it.copy(deletionConfirmation = info, showDeleteDialog = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionError = e.localizedMessage ?: "Failed to prepare deletion")
                }
            }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun executeDelete() {
        val current = _uiState.value.detail ?: return
        viewModelScope.launch {
            try {
                val success = deleteStreakWithConfirmationUseCase.executeDelete(current.type, current.id)
                _uiState.update { it.copy(isDeleted = success, showDeleteDialog = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        actionError = e.localizedMessage ?: "Failed to delete streak",
                        showDeleteDialog = false
                    )
                }
            }
        }
    }

    fun changeMonth(newMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = newMonth) }
    }
}
