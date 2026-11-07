package com.devson.NeverMiss.core.di

import com.devson.NeverMiss.data.repository.AppStreakRepositoryImpl
import com.devson.NeverMiss.data.repository.StreakRepositoryImpl
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        repositoryImpl: StreakRepositoryImpl
    ): StreakRepository

    @Binds
    @Singleton
    abstract fun bindAppStreakRepository(
        repositoryImpl: AppStreakRepositoryImpl
    ): AppStreakRepository
}
