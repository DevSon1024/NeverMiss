package com.devson.NeverMiss.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.usecase.StreakUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import com.devson.NeverMiss.core.util.AppClock

data class HomeState(
    val activeStreaks: List<Streak> = emptyList(),
    val pendingToday: List<Streak> = emptyList(),
    val completedToday: List<Streak> = emptyList(),
    val highestStreaks: List<Streak> = emptyList(),
    val recentlyMissed: List<Streak> = emptyList(),
    val archivedStreaks: List<Streak> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isGridView: Boolean = false,
    val showArchived: Boolean = false,
    val freezesRemaining: Int = 3
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakUseCases: StreakUseCases,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(false)
    private val _showArchived = MutableStateFlow(false)

    val state: StateFlow<HomeState> = combine(
        streakUseCases.getAllStreaks(),
        userPreferences.streakFreezesFlow,
        _searchQuery,
        _selectedCategory,
        _isGridView,
        _showArchived
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val streaks = array[0] as List<Streak>
        val freezes = array[1] as Int
        val search = array[2] as String
        val category = array[3] as String?
        val isGrid = array[4] as Boolean
        val showArchived = array[5] as Boolean

        val todayStr = StreakCalculator.formatDate(AppClock.now())
        val yesterday = AppClock.now().minusDays(1)
        val yesterdayStr = StreakCalculator.formatDate(yesterday)

        // Split active and archived
        val activeList = streaks.filter { !it.isArchived }
        val archivedList = streaks.filter { it.isArchived }

        // Apply filters
        val filteredActive = activeList.filter { streak ->
            val matchesSearch = streak.title.contains(search, ignoreCase = true) ||
                    streak.description.contains(search, ignoreCase = true)
            val matchesCategory = category == null || streak.category.name == category
            matchesSearch && matchesCategory
        }

        val filteredArchived = archivedList.filter { streak ->
            val matchesSearch = streak.title.contains(search, ignoreCase = true) ||
                    streak.description.contains(search, ignoreCase = true)
            val matchesCategory = category == null || streak.category.name == category
            matchesSearch && matchesCategory
        }

        // Categorize active streaks
        val completed = filteredActive.filter { it.lastCompletedDate == todayStr }
        
        val pending = filteredActive.filter { streak ->
            val isCompleted = streak.lastCompletedDate == todayStr
            val isScheduledToday = StreakCalculator.isScheduled(streak, AppClock.now())
            isScheduledToday && !isCompleted && !streak.isPaused
        }

        val highest = filteredActive.filter { it.currentStreak > 0 }
            .sortedByDescending { it.currentStreak }
            .take(3)

        // Missed recently: scheduled yesterday or today, not completed, and streak is 0 but longest > 0
        val missed = filteredActive.filter { streak ->
            val wasScheduledYesterday = StreakCalculator.isScheduled(streak, yesterday)
            val wasCompletedYesterday = streak.lastCompletedDate == yesterdayStr
            val isMissed = wasScheduledYesterday && !wasCompletedYesterday && streak.currentStreak == 0 && streak.longestStreak > 0
            isMissed && !streak.isPaused
        }

        HomeState(
            activeStreaks = filteredActive,
            pendingToday = pending,
            completedToday = completed,
            highestStreaks = highest,
            recentlyMissed = missed,
            archivedStreaks = filteredArchived,
            searchQuery = search,
            selectedCategory = category,
            isGridView = isGrid,
            showArchived = showArchived,
            freezesRemaining = freezes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun onToggleLayout() {
        _isGridView.value = !_isGridView.value
    }

    fun onToggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun completeStreak(streakId: Long) {
        viewModelScope.launch {
            val todayStr = StreakCalculator.formatDate(AppClock.now())
            streakUseCases.logCompletion(streakId, todayStr, CompletionStatus.COMPLETED)
        }
    }

    fun skipStreak(streakId: Long) {
        viewModelScope.launch {
            val todayStr = StreakCalculator.formatDate(AppClock.now())
            streakUseCases.logCompletion(streakId, todayStr, CompletionStatus.SKIPPED)
        }
    }

    fun freezeStreak(streakId: Long) {
        viewModelScope.launch {
            val todayStr = StreakCalculator.formatDate(AppClock.now())
            streakUseCases.logCompletion(streakId, todayStr, CompletionStatus.FROZEN)
        }
    }

    fun togglePauseStreak(streak: Streak) {
        viewModelScope.launch {
            val updated = streak.copy(isPaused = !streak.isPaused)
            streakUseCases.updateStreak(updated)
        }
    }

    fun toggleArchiveStreak(streak: Streak) {
        viewModelScope.launch {
            val updated = streak.copy(isArchived = !streak.isArchived)
            streakUseCases.updateStreak(updated)
        }
    }

    fun deleteStreak(streak: Streak) {
        viewModelScope.launch {
            streakUseCases.deleteStreak(streak)
        }
    }
}
