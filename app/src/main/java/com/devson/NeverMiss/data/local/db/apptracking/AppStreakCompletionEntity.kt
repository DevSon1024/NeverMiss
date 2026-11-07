package com.devson.NeverMiss.data.local.db.apptracking

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.ConfirmationStatus

@Entity(
    tableName = "app_streak_completions",
    foreignKeys = [
        ForeignKey(
            entity = TrackedAppEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackedAppId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackedAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackedAppId"]),
        Index(value = ["accountId"]),
        Index(value = ["completionDate"])
    ]
)
data class AppStreakCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackedAppId: Long,
    val accountId: Long? = null,
    val completionDate: String, // yyyy-MM-dd
    val detectedAt: Long = System.currentTimeMillis(),
    val completionMethod: String = CompletionMethod.AUTOMATIC_USAGE.name,
    val confirmationStatus: String = ConfirmationStatus.CONFIRMED.name
) {
    fun toDomain(): AppStreakCompletion = AppStreakCompletion(
        id = id,
        trackedAppId = trackedAppId,
        accountId = accountId,
        completionDate = completionDate,
        detectedAt = detectedAt,
        completionMethod = try {
            CompletionMethod.valueOf(completionMethod)
        } catch (e: Exception) {
            CompletionMethod.AUTOMATIC_USAGE
        },
        confirmationStatus = try {
            ConfirmationStatus.valueOf(confirmationStatus)
        } catch (e: Exception) {
            ConfirmationStatus.CONFIRMED
        }
    )

    companion object {
        fun fromDomain(domain: AppStreakCompletion): AppStreakCompletionEntity = AppStreakCompletionEntity(
            id = domain.id,
            trackedAppId = domain.trackedAppId,
            accountId = domain.accountId,
            completionDate = domain.completionDate,
            detectedAt = domain.detectedAt,
            completionMethod = domain.completionMethod.name,
            confirmationStatus = domain.confirmationStatus.name
        )
    }
}

