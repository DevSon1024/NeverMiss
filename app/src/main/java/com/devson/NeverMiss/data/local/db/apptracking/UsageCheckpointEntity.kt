package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint

@Entity(
    tableName = "usage_tracking_checkpoints",
    indices = [
        Index(value = ["sourceIdentifier"], unique = true)
    ]
)
data class UsageCheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceIdentifier: String = UsageCheckpoint.DEFAULT_SOURCE,
    val lastProcessedTimestamp: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UsageCheckpoint = UsageCheckpoint(
        id = id,
        sourceIdentifier = sourceIdentifier,
        lastProcessedTimestamp = lastProcessedTimestamp,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: UsageCheckpoint): UsageCheckpointEntity = UsageCheckpointEntity(
            id = domain.id,
            sourceIdentifier = domain.sourceIdentifier,
            lastProcessedTimestamp = domain.lastProcessedTimestamp,
            updatedAt = domain.updatedAt
        )
    }
}

