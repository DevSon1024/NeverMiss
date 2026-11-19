package com.devson.NeverMiss.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completion_history",
    foreignKeys = [
        ForeignKey(
            entity = StreakEntity::class,
            parentColumns = ["id"],
            childColumns = ["streakId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["streakId"]),
        Index(value = ["date"]),
        Index(value = ["streakId", "date"], unique = true)
    ]
)
data class CompletionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val streakId: Long,
    val date: String, // yyyy-MM-dd
    val status: String, // COMPLETED, SKIPPED, FROZEN, PAUSED
    val timestamp: Long // epoch millis of logging
)
