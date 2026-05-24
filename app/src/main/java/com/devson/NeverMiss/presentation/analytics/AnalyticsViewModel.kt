package com.devson.NeverMiss.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class StreakAnalyticsSummary(
    val id: Long,
    val title: String,
    val emoji: String,
    val color: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionPercentage: Double
)

data class AnalyticsState(
    val totalStreaks: Int = 0,
    val totalCompletions: Int = 0,
    val highestActiveStreak: Int = 0,
    val overallConsistency: Double = 0.0,
    val weeklyCompletionRate: List<Pair<String, Float>> = emptyList(), // Day -> Rate (0.0 to 1.0)
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val completionDensity: Map<String, Int> = emptyMap(), // yyyy-MM-dd -> count
    val summaries: List<StreakAnalyticsSummary> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: StreakRepository
) : ViewModel() {

    val state: StateFlow<AnalyticsState> = combine(
        repository.getAllStreaksFlow(),
        // Simple trick: combine with a flow that pulls all history or just pull history on updates
        // Since we want dynamic updates, we can trigger a refresh flow or load it asynchronously.
        // Let's just create a flow that updates when history changes. 
        // We'll create a flow that queries database directly.
        repository.getActiveStreaksFlow() // this forces updates when streaks/database changes
    ) { allStreaks, _ ->
        val summaries = mutableListOf<StreakAnalyticsSummary>()
        var totalCompletionsCount = 0
        var maxStreak = 0
        var totalScheduledDays = 0L
        var totalCompletedDays = 0L
        val densityMap = mutableMapOf<String, Int>()
        val categoryMap = mutableMapOf<String, Int>()

        for (streak in allStreaks) {
            val history = repository.getHistoryForStreak(streak.id)
            val completedLogs = history.filter { it.status == CompletionStatus.COMPLETED }
            totalCompletionsCount += completedLogs.size

            // Populate density map
            completedLogs.forEach { log ->
                densityMap[log.date] = (densityMap[log.date] ?: 0) + 1
            }

            if (streak.currentStreak > maxStreak) {
                maxStreak = streak.currentStreak
            }

            // Calculate consistency for this streak
            val creationDate = StreakCalculator.parseDate(streak.creationDate)
            val today = LocalDate.now()
            var scheduledCount = 0
            var completedCount = 0

            if (!today.isBefore(creationDate)) {
                val daysCount = ChronoUnit.DAYS.between(creationDate, today)
                for (i in 0..daysCount) {
                    val date = creationDate.plusDays(i)
                    if (StreakCalculator.isScheduled(streak, date)) {
                        scheduledCount++
                        val hasCompleted = history.any { it.date == StreakCalculator.formatDate(date) && it.status == CompletionStatus.COMPLETED }
                        if (hasCompleted) {
                            completedCount++
                        }
                    }
                }
            }

            totalScheduledDays += scheduledCount
            totalCompletedDays += completedCount

            val rate = if (scheduledCount > 0) {
                (completedCount.toDouble() / scheduledCount) * 100.0
            } else 0.0

            summaries.add(
                StreakAnalyticsSummary(
                    id = streak.id,
                    title = streak.title,
                    emoji = streak.emoji,
                    color = streak.color,
                    currentStreak = streak.currentStreak,
                    longestStreak = streak.longestStreak,
                    completionPercentage = rate
                )
            )

            // Category breakdown
            val catName = streak.category.displayName
            categoryMap[catName] = (categoryMap[catName] ?: 0) + completedLogs.size
        }

        // Calculate weekly rate for the last 7 days
        val weeklyRates = mutableListOf<Pair<String, Float>>()
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = StreakCalculator.formatDate(date)
            
            var scheduled = 0
            var completed = 0
            for (streak in allStreaks) {
                if (StreakCalculator.isScheduled(streak, date)) {
                    scheduled++
                    val history = repository.getHistoryForStreak(streak.id)
                    val wasDone = history.any { it.date == dateStr && it.status == CompletionStatus.COMPLETED }
                    if (wasDone) {
                        completed++
                    }
                }
            }

            val rate = if (scheduled > 0) (completed.toFloat() / scheduled) else 0f
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            weeklyRates.add(dayName to rate)
        }

        val overallRate = if (totalScheduledDays > 0) {
            (totalCompletedDays.toDouble() / totalScheduledDays) * 100.0
        } else 0.0

        AnalyticsState(
            totalStreaks = allStreaks.size,
            totalCompletions = totalCompletionsCount,
            highestActiveStreak = maxStreak,
            overallConsistency = overallRate,
            weeklyCompletionRate = weeklyRates,
            categoryBreakdown = categoryMap,
            completionDensity = densityMap,
            summaries = summaries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )
}
