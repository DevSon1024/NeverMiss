package com.devson.NeverMiss.viewmodel.appstreaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.data.local.apps.InstalledAppInfo
import com.devson.NeverMiss.domain.usecase.appstreaks.AddTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.CheckUsagePermissionUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.GetInstalledAppsUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.RemoveTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ToggleTrackedAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppPickerState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = false,
    val showSystemApps: Boolean = false,
    val trackedCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class InstalledAppPickerViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val addTrackedAppUseCase: AddTrackedAppUseCase,
    private val toggleTrackedAppUseCase: ToggleTrackedAppUseCase,
    private val removeTrackedAppUseCase: RemoveTrackedAppUseCase,
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InstalledAppPickerState())
    val state: StateFlow<InstalledAppPickerState> = _state.asStateFlow()

    init {
        refreshPermissionAndApps()
    }

    fun refreshPermissionAndApps() {
        val hasPermission = checkUsagePermissionUseCase()
        _state.update { it.copy(hasUsageAccess = hasPermission) }
        loadApps()
    }

    fun checkPermissionOnly() {
        val hasPermission = checkUsagePermissionUseCase()
        _state.update { it.copy(hasUsageAccess = hasPermission) }
    }

    fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val apps = getInstalledAppsUseCase()
                val trackedCount = apps.count { it.isTracked && it.isTrackingEnabled }
                _state.update { current ->
                    current.copy(
                        apps = apps,
                        filteredApps = filterApps(apps, current.searchQuery, current.showSystemApps),
                        isLoading = false,
                        trackedCount = trackedCount
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to load applications: ${e.localizedMessage}") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { current ->
            current.copy(
                searchQuery = query,
                filteredApps = filterApps(current.apps, query, current.showSystemApps)
            )
        }
    }

    fun toggleShowSystemApps() {
        _state.update { current ->
            val newShowSystem = !current.showSystemApps
            current.copy(
                showSystemApps = newShowSystem,
                filteredApps = filterApps(current.apps, current.searchQuery, newShowSystem)
            )
        }
    }

    fun toggleTracking(app: InstalledAppInfo) {
        viewModelScope.launch {
            try {
                if (!app.isTracked) {
                    // Add app to tracking
                    val newId = addTrackedAppUseCase(app.packageName, app.appName)
                    updateAppInList(app.packageName) { it.copy(isTracked = true, trackedAppId = newId, isTrackingEnabled = true) }
                } else if (app.isTrackingEnabled) {
                    // Currently enabled -> toggle to disabled
                    val trackedId = app.trackedAppId
                    if (trackedId != null) {
                        toggleTrackedAppUseCase(trackedId)
                        updateAppInList(app.packageName) { it.copy(isTrackingEnabled = false) }
                    }
                } else {
                    // Currently disabled -> toggle back to enabled
                    val trackedId = app.trackedAppId
                    if (trackedId != null) {
                        toggleTrackedAppUseCase(trackedId)
                        updateAppInList(app.packageName) { it.copy(isTrackingEnabled = true) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Could not update tracking: ${e.localizedMessage}") }
            }
        }
    }

    fun removeTracking(app: InstalledAppInfo) {
        viewModelScope.launch {
            val trackedId = app.trackedAppId ?: return@launch
            try {
                removeTrackedAppUseCase(trackedId)
                updateAppInList(app.packageName) { it.copy(isTracked = false, trackedAppId = null, isTrackingEnabled = false) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Could not remove app: ${e.localizedMessage}") }
            }
        }
    }

    private fun updateAppInList(packageName: String, transform: (InstalledAppInfo) -> InstalledAppInfo) {
        _state.update { current ->
            val updatedApps = current.apps.map { if (it.packageName == packageName) transform(it) else it }
            val trackedCount = updatedApps.count { it.isTracked && it.isTrackingEnabled }
            current.copy(
                apps = updatedApps,
                filteredApps = filterApps(updatedApps, current.searchQuery, current.showSystemApps),
                trackedCount = trackedCount
            )
        }
    }

    private fun filterApps(
        apps: List<InstalledAppInfo>,
        query: String,
        showSystem: Boolean
    ): List<InstalledAppInfo> {
        val trimmed = query.trim()
        return apps.filter { app ->
            val matchesSystem = showSystem || !app.isSystemApp || app.isTracked
            val matchesQuery = trimmed.isEmpty() ||
                app.appName.contains(trimmed, ignoreCase = true) ||
                app.packageName.contains(trimmed, ignoreCase = true)
            matchesSystem && matchesQuery
        }
    }
}

