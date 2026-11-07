package com.devson.NeverMiss.viewmodel.appstreaks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.apptracking.permission.UsageAccessPermissionHelper
import com.devson.NeverMiss.domain.model.apptracking.TrackingDiagnosticsInfo
import com.devson.NeverMiss.domain.usecase.appstreaks.GetTrackingDiagnosticsUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ProcessAppUsageEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TrackingDiagnosticsUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val scanResultBanner: String? = null,
    val info: TrackingDiagnosticsInfo? = null
)

@HiltViewModel
class TrackingDiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTrackingDiagnosticsUseCase: GetTrackingDiagnosticsUseCase,
    private val processAppUsageEventsUseCase: ProcessAppUsageEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingDiagnosticsUiState())
    val uiState: StateFlow<TrackingDiagnosticsUiState> = _uiState.asStateFlow()

    init {
        loadDiagnostics()
    }

    fun loadDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val info = getTrackingDiagnosticsUseCase()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    info = info
                )
            }
        }
    }

    fun runManualScan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanResultBanner = null) }
            try {
                val summary = processAppUsageEventsUseCase()
                val banner = "Scan Complete: ${summary.detectedAppsCount} apps detected, ${summary.newlyCompletedAppsCount} new completions, ${summary.pendingConfirmationsQueued} confirmations queued."
                val updatedInfo = getTrackingDiagnosticsUseCase()
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResultBanner = banner,
                        info = updatedInfo
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResultBanner = "Scan Error: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun openUsageAccessSettings() {
        try {
            val intent = UsageAccessPermissionHelper.createUsageAccessIntent(context)
            context.startActivity(intent)
        } catch (ignored: Exception) {
        }
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {
            }
        }
    }
}
