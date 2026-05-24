package com.devson.NeverMiss.core.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.NeverMiss.domain.usecase.CheckAndResetMissedStreaksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyStreakWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkAndResetMissedStreaksUseCase: CheckAndResetMissedStreaksUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            checkAndResetMissedStreaksUseCase()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
