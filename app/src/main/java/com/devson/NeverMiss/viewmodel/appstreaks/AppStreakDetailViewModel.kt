package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.usecase.appstreaks.AddTrackedAccountUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.AppStreakDetail
import com.devson.NeverMiss.domain.usecase.appstreaks.DeleteTrackedAccountUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.GetAppStreakDetailsUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.RemoveTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ToggleTrackedAppUseCase
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
data class AppStreakDetailUiState(
    val isLoading: Boolean = true,
    val detail: AppStreakDetail? = null,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val isAppDeleted: Boolean = false,
    val showAddAccountDialog: Boolean = false,
    val editingAccount: TrackedAccount? = null
)

@HiltViewModel
class AppStreakDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAppStreakDetailsUseCase: GetAppStreakDetailsUseCase,
    private val addTrackedAccountUseCase: AddTrackedAccountUseCase,
    private val updateTrackedAccountUseCase: UpdateTrackedAccountUseCase,
    private val deleteTrackedAccountUseCase: DeleteTrackedAccountUseCase,
    private val toggleTrackedAppUseCase: ToggleTrackedAppUseCase,
    private val removeTrackedAppUseCase: RemoveTrackedAppUseCase
) : ViewModel() {

    private var currentAppId: Long = savedStateHandle.get<Long>("appId")
        ?: savedStateHandle.get<String>("appId")?.toLongOrNull()
        ?: -1L

    private val _uiState = MutableStateFlow(AppStreakDetailUiState())
    val uiState: StateFlow<AppStreakDetailUiState> = _uiState.asStateFlow()

    init {
        if (currentAppId > 0) {
            loadDetails(currentAppId)
        }
    }

    fun loadDetails(appId: Long = currentAppId) {
        currentAppId = appId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val detail = getAppStreakDetailsUseCase(appId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    detail = detail
                )
            }
        }
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedYearMonth = yearMonth) }
    }

    fun toggleAppTracking() {
        if (currentAppId <= 0) return
        viewModelScope.launch {
            toggleTrackedAppUseCase(currentAppId)
            loadDetails(currentAppId)
        }
    }

    fun deleteAppTracking(onDeleted: () -> Unit = {}) {
        if (currentAppId <= 0) return
        viewModelScope.launch {
            removeTrackedAppUseCase(currentAppId)
            _uiState.update { it.copy(isAppDeleted = true) }
            onDeleted()
        }
    }

    fun showAddAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showAddAccountDialog = show) }
    }

    fun setEditingAccount(account: TrackedAccount?) {
        _uiState.update { it.copy(editingAccount = account) }
    }

    fun addAccount(accountName: String, initialStreak: Int = 0) {
        if (currentAppId <= 0) return
        viewModelScope.launch {
            addTrackedAccountUseCase(currentAppId, accountName, initialStreak)
            loadDetails(currentAppId)
            showAddAccountDialog(false)
        }
    }

    fun renameAccount(accountId: Long, newName: String) {
        viewModelScope.launch {
            updateTrackedAccountUseCase.renameAccount(accountId, newName)
            loadDetails(currentAppId)
            setEditingAccount(null)
        }
    }

    fun toggleAccountTracking(accountId: Long) {
        viewModelScope.launch {
            updateTrackedAccountUseCase.toggleAccountEnabled(accountId)
            loadDetails(currentAppId)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            deleteTrackedAccountUseCase.deleteById(accountId)
            loadDetails(currentAppId)
        }
    }
}

