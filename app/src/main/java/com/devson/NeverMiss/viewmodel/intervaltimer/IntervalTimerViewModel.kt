package com.devson.NeverMiss.viewmodel.intervaltimer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.usecase.intervaltimer.IntervalTimerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class IntervalTimerUiState(
    val timers: List<IntervalTimer> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val filterStatus: IntervalTimerStatus? = null
)

@HiltViewModel
class IntervalTimerViewModel @Inject constructor(
    private val useCases: IntervalTimerUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntervalTimerUiState())
    val uiState: StateFlow<IntervalTimerUiState> = _uiState.asStateFlow()

    val timers: StateFlow<List<IntervalTimer>> = _uiState
        .map { it.timers }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private var observeJob: Job? = null

    init {
        observeTimers()
    }

    private fun observeTimers() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val status = _uiState.value.filterStatus
            val sourceFlow = if (status == null) {
                useCases.getAllTimers()
            } else {
                useCases.getTimersByStatus(status)
            }

            sourceFlow.catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmpty = true,
                        errorMessage = throwable.message ?: "Failed to observe interval timers"
                    )
                }
            }.collect { list ->
                _uiState.update {
                    it.copy(
                        timers = list,
                        isLoading = false,
                        isEmpty = list.isEmpty()
                    )
                }
            }
        }
    }

    fun createTimer(timer: IntervalTimer, onResult: ((Result<Long>) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val id = useCases.createTimer(timer)
                _uiState.update { it.copy(isLoading = false) }
                onResult?.invoke(Result.success(id))
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to create interval timer"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun createTimer(
        title: String,
        mode: IntervalTimerMode,
        durationMillis: Long = 0L,
        intervalMillis: Long = 0L,
        repeatLimit: Int? = null,
        onResult: ((Result<Long>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val id = useCases.createTimer(
                    title = title,
                    mode = mode,
                    durationMillis = durationMillis,
                    intervalMillis = intervalMillis,
                    repeatLimit = repeatLimit
                )
                _uiState.update { it.copy(isLoading = false) }
                onResult?.invoke(Result.success(id))
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to create interval timer"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun updateTimer(timer: IntervalTimer, onResult: ((Result<Boolean>) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val success = useCases.updateTimer(timer)
                if (success) {
                    _uiState.update { it.copy(isLoading = false) }
                    onResult?.invoke(Result.success(true))
                } else {
                    val err = "Timer not found"
                    _uiState.update { it.copy(isLoading = false, errorMessage = err) }
                    onResult?.invoke(Result.failure(IllegalArgumentException(err)))
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to update interval timer"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun deleteTimer(timerId: Long, onResult: ((Result<Boolean>) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val success = useCases.deleteTimer(timerId)
                if (success) {
                    _uiState.update { it.copy(isLoading = false) }
                    onResult?.invoke(Result.success(true))
                } else {
                    val err = "Timer not found"
                    _uiState.update { it.copy(isLoading = false, errorMessage = err) }
                    onResult?.invoke(Result.failure(IllegalArgumentException(err)))
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to delete interval timer"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun setFilterStatus(status: IntervalTimerStatus?) {
        _uiState.update { it.copy(filterStatus = status, isLoading = true) }
        observeTimers()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        observeTimers()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
