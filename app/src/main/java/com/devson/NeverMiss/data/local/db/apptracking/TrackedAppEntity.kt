package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp

@Entity(
    tableName = "tracked_apps",
    indices = [
        Index(value = ["packageName"], unique = true)
    ]
)
data class TrackedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val trackingCreatedAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "",
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val reminderTime: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TrackedApp = TrackedApp(
        id = id,
        packageName = packageName,
        appName = appName,
        isEnabled = isEnabled,
        trackingCreatedAt = trackingCreatedAt,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedDate = lastCompletedDate,
        isPaused = isPaused,
        isArchived = isArchived,
        reminderTime = reminderTime,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: TrackedApp): TrackedAppEntity = TrackedAppEntity(
            id = domain.id,
            packageName = domain.packageName,
            appName = domain.appName,
            isEnabled = domain.isEnabled,
            trackingCreatedAt = domain.trackingCreatedAt,
            currentStreak = domain.currentStreak,
            longestStreak = domain.longestStreak,
            lastCompletedDate = domain.lastCompletedDate,
            isPaused = domain.isPaused,
            isArchived = domain.isArchived,
            reminderTime = domain.reminderTime,
            updatedAt = domain.updatedAt
        )
    }
}
