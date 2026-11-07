package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.AppStreakStats
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.usecase.appstreaks.DeleteTrackedAccountUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.UpdateTrackedAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@Immutable
data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val account: TrackedAccount? = null,
    val app: TrackedApp? = null,
    val stats: AppStreakStats? = null,
    val completions: List<AppStreakCompletion> = emptyList(),
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val isAccountDeleted: Boolean = false,
    val showRenameDialog: Boolean = false
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appStreakRepository: AppStreakRepository,
    private val updateTrackedAccountUseCase: UpdateTrackedAccountUseCase,
    private val deleteTrackedAccountUseCase: DeleteTrackedAccountUseCase
) : ViewModel() {

    private var currentAppId: Long = savedStateHandle.get<Long>("appId")
        ?: savedStateHandle.get<String>("appId")?.toLongOrNull()
        ?: -1L

    private var currentAccountId: Long = savedStateHandle.get<Long>("accountId")
        ?: savedStateHandle.get<String>("accountId")?.toLongOrNull()
        ?: -1L

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    init {
        if (currentAppId > 0 && currentAccountId > 0) {
            loadAccount(currentAppId, currentAccountId)
        }
    }

    fun loadAccount(appId: Long = currentAppId, accountId: Long = currentAccountId) {
        currentAppId = appId
        currentAccountId = accountId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val app = appStreakRepository.getTrackedAppById(appId)
            val account = appStreakRepository.getAccountById(accountId)
            val completions = appStreakRepository.getCompletionsForAccount(accountId)
            val stats = if (account != null) {
                AppStreakCalculator.calculateAccountStreak(account, completions)
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    account = account,
                    app = app,
                    stats = stats,
                    completions = completions
                )
            }
        }
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedYearMonth = yearMonth) }
    }

    fun setShowRenameDialog(show: Boolean) {
        _uiState.update { it.copy(showRenameDialog = show) }
    }

    fun renameAccount(newName: String) {
        if (currentAccountId <= 0) return
        viewModelScope.launch {
            updateTrackedAccountUseCase.renameAccount(currentAccountId, newName)
            loadAccount(currentAppId, currentAccountId)
            setShowRenameDialog(false)
        }
    }

    fun toggleTracking() {
        if (currentAccountId <= 0) return
        viewModelScope.launch {
            updateTrackedAccountUseCase.toggleAccountEnabled(currentAccountId)
            loadAccount(currentAppId, currentAccountId)
        }
    }

    fun deleteAccount(onDeleted: () -> Unit = {}) {
        if (currentAccountId <= 0) return
        viewModelScope.launch {
            deleteTrackedAccountUseCase.deleteById(currentAccountId)
            _uiState.update { it.copy(isAccountDeleted = true) }
            onDeleted()
        }
    }
}

