package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.apptracking.notification.AccountConfirmationNotifier
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmationWithDetails
import com.devson.NeverMiss.domain.usecase.appstreaks.ConfirmAccountSessionUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.GetActivePendingConfirmationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountConfirmationUiState(
    val pendingConfirmations: List<PendingConfirmationWithDetails> = emptyList(),
    val activeConfirmation: PendingConfirmationWithDetails? = null,
    val selectedAccountIds: Set<Long> = emptySet(),
    val isProcessing: Boolean = false
)

@HiltViewModel
class AccountConfirmationViewModel @Inject constructor(
    private val getActivePendingConfirmationsUseCase: GetActivePendingConfirmationsUseCase,
    private val confirmAccountSessionUseCase: ConfirmAccountSessionUseCase,
    private val accountConfirmationNotifier: AccountConfirmationNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountConfirmationUiState())
    val uiState: StateFlow<AccountConfirmationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getActivePendingConfirmationsUseCase().collect { pendingList ->
                _uiState.update { current ->
                    val nextActive = when {
                        current.activeConfirmation != null && pendingList.any { it.confirmation.id == current.activeConfirmation.confirmation.id } -> {
                            pendingList.firstOrNull { it.confirmation.id == current.activeConfirmation.confirmation.id }
                        }
                        pendingList.isNotEmpty() -> pendingList.first()
                        else -> null
                    }
                    current.copy(
                        pendingConfirmations = pendingList,
                        activeConfirmation = nextActive,
                        selectedAccountIds = if (nextActive?.confirmation?.id != current.activeConfirmation?.confirmation?.id) {
                            emptySet()
                        } else {
                            current.selectedAccountIds
                        }
                    )
                }
            }
        }
    }

    fun toggleAccountSelection(accountId: Long) {
        _uiState.update { current ->
            val updated = if (current.selectedAccountIds.contains(accountId)) {
                current.selectedAccountIds - accountId
            } else {
                current.selectedAccountIds + accountId
            }
            current.copy(selectedAccountIds = updated)
        }
    }

    fun selectActiveConfirmation(confirmation: PendingConfirmationWithDetails) {
        _uiState.update {
            it.copy(
                activeConfirmation = confirmation,
                selectedAccountIds = emptySet()
            )
        }
    }

    fun confirmCurrentSelection() {
        val current = _uiState.value
        val confirmation = current.activeConfirmation ?: return
        if (current.isProcessing) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                confirmAccountSessionUseCase(
                    pendingConfirmationId = confirmation.confirmation.id,
                    selectedAccountIds = current.selectedAccountIds.toList(),
                    dismissed = false
                )
                accountConfirmationNotifier.cancelNotification(confirmation.confirmation.id)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun dismissCurrent() {
        val current = _uiState.value
        val confirmation = current.activeConfirmation ?: return
        if (current.isProcessing) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                confirmAccountSessionUseCase(
                    pendingConfirmationId = confirmation.confirmation.id,
                    selectedAccountIds = emptyList(),
                    dismissed = true
                )
                accountConfirmationNotifier.cancelNotification(confirmation.confirmation.id)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }
}

