package com.devson.NeverMiss.core.di

import android.content.Context
import androidx.room.Room
import com.devson.NeverMiss.data.local.db.NeverMissDatabase
import com.devson.NeverMiss.data.local.db.StreakDao
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
        ).fallbackToDestructiveMigration() // Simple database reset for development. For release, implement migrations.
         .build()
    }

    @Provides
    @Singleton
    fun provideStreakDao(database: NeverMissDatabase): StreakDao {
        return database.streakDao()
    }
}
