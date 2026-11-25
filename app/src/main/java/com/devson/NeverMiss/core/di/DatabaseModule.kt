package com.devson.NeverMiss.core.di

import android.content.Context
import androidx.room.Room
import com.devson.NeverMiss.data.local.db.IntervalTimerDao
import com.devson.NeverMiss.data.local.db.NeverMissDatabase
import com.devson.NeverMiss.data.local.db.StreakDao
import com.devson.NeverMiss.data.local.db.apptracking.AppStreakDao
import com.devson.NeverMiss.data.local.db.migrations.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NeverMissDatabase {
        return Room.databaseBuilder(
            context,
            NeverMissDatabase::class.java,
            "nevermiss_database"
        ).addMigrations(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5
        ).build()
    }

    @Provides
    @Singleton
    fun provideStreakDao(database: NeverMissDatabase): StreakDao {
        return database.streakDao()
    }

    @Provides
    @Singleton
    fun provideAppStreakDao(database: NeverMissDatabase): AppStreakDao {
        return database.appStreakDao()
    }

    @Provides
    @Singleton
    fun provideIntervalTimerDao(database: NeverMissDatabase): IntervalTimerDao {
        return database.intervalTimerDao()
    }
}
