package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmationStatus

@Entity(
    tableName = "pending_account_confirmations",
    foreignKeys = [
        ForeignKey(
            entity = TrackedAppEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackedAppId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackedAppId"])
    ]
)
data class PendingConfirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackedAppId: Long,
    val sessionTimestamp: Long,
    val sessionDate: String, // yyyy-MM-dd
    val status: String = PendingConfirmationStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + PendingConfirmation.DEFAULT_EXPIRY_MILLIS
) {
    fun toDomain(): PendingConfirmation = PendingConfirmation(
        id = id,
        trackedAppId = trackedAppId,
        sessionTimestamp = sessionTimestamp,
        sessionDate = sessionDate,
        status = try {
            PendingConfirmationStatus.valueOf(status)
        } catch (e: Exception) {
            PendingConfirmationStatus.PENDING
        },
        createdAt = createdAt,
        expiresAt = expiresAt
    )

    companion object {
        fun fromDomain(domain: PendingConfirmation): PendingConfirmationEntity = PendingConfirmationEntity(
            id = domain.id,
            trackedAppId = domain.trackedAppId,
            sessionTimestamp = domain.sessionTimestamp,
            sessionDate = domain.sessionDate,
            status = domain.status.name,
            createdAt = domain.createdAt,
            expiresAt = domain.expiresAt
        )
    }
}

