package com.devson.NeverMiss.domain.model

import androidx.compose.runtime.Immutable
import com.devson.NeverMiss.data.local.db.CompletionHistoryEntity

@Immutable
data class CompletionHistory(
    val id: Long = 0L,
    val streakId: Long,
    val date: String, // yyyy-MM-dd
    val status: CompletionStatus,
    val timestamp: Long
) {
    fun toEntity(): CompletionHistoryEntity {
        return CompletionHistoryEntity(
            id = id,
            streakId = streakId,
            date = date,
            status = status.name,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromEntity(entity: CompletionHistoryEntity): CompletionHistory {
            val statusEnum = try {
                CompletionStatus.valueOf(entity.status)
            } catch (e: IllegalArgumentException) {
                CompletionStatus.COMPLETED
            }
            return CompletionHistory(
                id = entity.id,
                streakId = entity.streakId,
                date = entity.date,
                status = statusEnum,
                timestamp = entity.timestamp
            )
        }
    }
}

enum class CompletionStatus {
    COMPLETED, SKIPPED, FROZEN
}
