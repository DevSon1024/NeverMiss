package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount

@Entity(
    tableName = "tracked_accounts",
    foreignKeys = [
        ForeignKey(
            entity = TrackedAppEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackedAppId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackedAppId"]),
        Index(value = ["trackedAppId", "accountName"], unique = true)
    ]
)
data class TrackedAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackedAppId: Long,
    val accountName: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "",
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TrackedAccount = TrackedAccount(
        id = id,
        trackedAppId = trackedAppId,
        accountName = accountName,
        isEnabled = isEnabled,
        createdAt = createdAt,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedDate = lastCompletedDate,
        isPaused = isPaused,
        isArchived = isArchived,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: TrackedAccount): TrackedAccountEntity = TrackedAccountEntity(
            id = domain.id,
            trackedAppId = domain.trackedAppId,
            accountName = domain.accountName,
            isEnabled = domain.isEnabled,
            createdAt = domain.createdAt,
            currentStreak = domain.currentStreak,
            longestStreak = domain.longestStreak,
            lastCompletedDate = domain.lastCompletedDate,
            isPaused = domain.isPaused,
            isArchived = domain.isArchived,
            updatedAt = domain.updatedAt
        )
    }
}
