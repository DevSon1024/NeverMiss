package com.devson.NeverMiss.domain.model.apptracking

import androidx.compose.runtime.Immutable
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakMetadata
import com.devson.NeverMiss.domain.model.StreakStatus
import com.devson.NeverMiss.domain.model.StreakType

@Immutable
data class TrackedApp(
    override val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val trackingCreatedAt: Long = System.currentTimeMillis(),
    override val currentStreak: Int = 0,
    override val longestStreak: Int = 0,
    override val lastCompletedDate: String = "",
    override val isPaused: Boolean = false,
    override val isArchived: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : StreakMetadata {

    override val type: StreakType get() = StreakType.APP
    override val name: String get() = appName
    override val description: String get() = packageName
    override val status: StreakStatus
        get() = when {
            isArchived -> StreakStatus.ARCHIVED
            isPaused -> StreakStatus.PAUSED
            lastCompletedDate == StreakCalculator.formatDate(AppClock.now()) -> StreakStatus.COMPLETED_TODAY
            else -> StreakStatus.ACTIVE
        }
}
