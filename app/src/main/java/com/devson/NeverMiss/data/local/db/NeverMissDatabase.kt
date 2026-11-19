package com.devson.NeverMiss.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.NeverMiss.data.local.db.apptracking.AppStreakCompletionEntity
import com.devson.NeverMiss.data.local.db.apptracking.AppStreakDao
import com.devson.NeverMiss.data.local.db.apptracking.PendingConfirmationEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAccountEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAppEntity
import com.devson.NeverMiss.data.local.db.apptracking.UsageCheckpointEntity

@Database(
    entities = [
        StreakEntity::class,
        CompletionHistoryEntity::class,
        TrackedAppEntity::class,
        TrackedAccountEntity::class,
        AppStreakCompletionEntity::class,
        UsageCheckpointEntity::class,
        PendingConfirmationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NeverMissDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
    abstract fun appStreakDao(): AppStreakDao
}
