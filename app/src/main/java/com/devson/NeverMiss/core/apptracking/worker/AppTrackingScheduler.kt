package com.devson.NeverMiss.core.apptracking.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTrackingScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val PERIODIC_WORK_NAME = AppUsageReconciliationWorker.WORK_NAME
        const val ONETIME_WORK_NAME = "${AppUsageReconciliationWorker.WORK_NAME}_IMMEDIATE"
        const val RECONCILIATION_INTERVAL_MINUTES = 15L // Minimum interval permitted by Android WorkManager
    }

    fun schedulePeriodicReconciliation() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AppUsageReconciliationWorker>(
            RECONCILIATION_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun triggerImmediateReconciliation() {
        val workRequest = OneTimeWorkRequestBuilder<AppUsageReconciliationWorker>()
            .build()

        workManager.enqueueUniqueWork(
            ONETIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReconciliation() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(ONETIME_WORK_NAME)
    }
}

