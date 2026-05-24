package com.devson.NeverMiss.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StreakEntity::class, CompletionHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NeverMissDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
}
