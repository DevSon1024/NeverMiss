package com.devson.NeverMiss.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeAppStreakItem(
    val app: TrackedApp,
    val accountCount: Int,
    val accountSummary: String,
    val isCompletedToday: Boolean
)

@Immutable
data class HomeState(
    val normalStreaks: List<Streak> = emptyList(),
    val appStreaks: List<HomeAppStreakItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val state: StateFlow<HomeState> = combine(
        streakRepository.getActiveStreaksFlow(),
        appStreakRepository.getActiveTrackedAppsFlow(),
        appStreakRepository.getAllAccountsFlow(),
        _searchQuery
    ) { activeStreaks, activeApps, allAccounts, search ->
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
            val accountSummary = when {
                appAccounts.isEmpty() -> "No accounts configured"
                appAccounts.size == 1 -> appAccounts.first().accountName
                else -> "${appAccounts.size} accounts (${appAccounts.joinToString(", ") { it.accountName }})"
            }
            HomeAppStreakItem(
                app = app,
                accountCount = appAccounts.size,
                accountSummary = accountSummary,
                isCompletedToday = app.lastCompletedDate == todayStr
            )
        }

        HomeState(
            normalStreaks = filteredNormal,
            appStreaks = filteredApps,
            searchQuery = search,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
