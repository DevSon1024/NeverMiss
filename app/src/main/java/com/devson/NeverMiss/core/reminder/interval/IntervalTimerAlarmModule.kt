package com.devson.NeverMiss.core.reminder.interval

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IntervalTimerAlarmModule {

    @Binds
    @Singleton
    abstract fun bindIntervalTimerAlarmScheduler(
        impl: IntervalTimerAlarmSchedulerImpl
    ): IntervalTimerAlarmScheduler
}
