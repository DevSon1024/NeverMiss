package com.devson.NeverMiss

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.BackoffPolicy
import com.devson.NeverMiss.core.apptracking.worker.AppTrackingScheduler
import com.devson.NeverMiss.core.reminder.DailyStreakWorker
import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.core.reminder.interval.IntervalTimerRecoveryCoordinator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appTrackingScheduler: AppTrackingScheduler

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var intervalTimerRecoveryCoordinator: IntervalTimerRecoveryCoordinator

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleDailyStreakCheck()
        appTrackingScheduler.schedulePeriodicReconciliation()
        applicationScope.launch {
            try {
                reminderScheduler.scheduleAllActiveReminders()
                intervalTimerRecoveryCoordinator.recoverActiveTimers()
            } catch (_: Throwable) {
            }
        }
    }

    private fun scheduleDailyStreakCheck() {
        val workRequest = PeriodicWorkRequestBuilder<DailyStreakWorker>(2, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyStreakWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
