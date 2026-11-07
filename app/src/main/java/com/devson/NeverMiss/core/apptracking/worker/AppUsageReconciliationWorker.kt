package com.devson.NeverMiss.core.apptracking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.NeverMiss.domain.usecase.appstreaks.CheckAndResetMissedAppStreaksUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ExpirePendingConfirmationsUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ProcessAppUsageEventsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AppUsageReconciliationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processAppUsageEventsUseCase: ProcessAppUsageEventsUseCase,
    private val expirePendingConfirmationsUseCase: ExpirePendingConfirmationsUseCase,
    private val checkAndResetMissedAppStreaksUseCase: CheckAndResetMissedAppStreaksUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "AppUsageReconciliationWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Process usage events
            processAppUsageEventsUseCase()
            // 2. Expire old pending account confirmations
            expirePendingConfirmationsUseCase()
            // 3. Reset broken app streaks
            checkAndResetMissedAppStreaksUseCase()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

