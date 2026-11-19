package com.devson.NeverMiss.viewmodel.archive

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import com.devson.NeverMiss.domain.usecase.common.DeleteStreakWithConfirmationUseCase
import com.devson.NeverMiss.domain.usecase.common.DeletionConfirmation
import com.devson.NeverMiss.domain.usecase.common.ToggleStreakArchiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ArchiveState(
    val archivedNormalStreaks: List<Streak> = emptyList(),
    val archivedAppStreaks: List<TrackedApp> = emptyList(),
    val isLoading: Boolean = false,
    val deletionConfirmation: DeletionConfirmation? = null,
    val showDeleteDialog: Boolean = false,
    val actionError: String? = null
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository,
    private val toggleStreakArchiveUseCase: ToggleStreakArchiveUseCase,
    private val deleteStreakWithConfirmationUseCase: DeleteStreakWithConfirmationUseCase
) : ViewModel() {

    private val _dialogState = MutableStateFlow<Pair<DeletionConfirmation?, Boolean>>(Pair(null, false))
    private val _actionError = MutableStateFlow<String?>(null)

    val state: StateFlow<ArchiveState> = combine(
        streakRepository.getAllArchivedStreaksFlow(),
        appStreakRepository.getArchivedTrackedAppsFlow(),
        _dialogState,
        _actionError
    ) { archivedNormal, archivedApps, dialog, error ->
        ArchiveState(
            archivedNormalStreaks = archivedNormal,
            archivedAppStreaks = archivedApps,
            isLoading = false,
            deletionConfirmation = dialog.first,
            showDeleteDialog = dialog.second,
            actionError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArchiveState(isLoading = true)
    )

    fun restoreStreak(type: StreakType, id: Long) {
        viewModelScope.launch {
            try {
                toggleStreakArchiveUseCase(type, id)
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to restore streak"
            }
        }
    }

    fun requestDelete(type: StreakType, id: Long) {
        viewModelScope.launch {
            val confirmation = deleteStreakWithConfirmationUseCase.getConfirmationInfo(type, id)
            _dialogState.value = Pair(confirmation, true)
        }
    }

    fun dismissDeleteDialog() {
        _dialogState.value = Pair(null, false)
    }

    fun confirmDelete() {
        val confirmation = _dialogState.value.first ?: return
        viewModelScope.launch {
            try {
                deleteStreakWithConfirmationUseCase.executeDelete(confirmation.type, confirmation.id)
                _dialogState.value = Pair(null, false)
            } catch (e: Exception) {
                _dialogState.value = Pair(null, false)
                _actionError.value = e.localizedMessage ?: "Failed to delete streak"
            }
        }
    }
}
