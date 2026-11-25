package com.devson.NeverMiss.core.reminder.interval

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AlarmSchedulingResult {
    data class Success(val isExact: Boolean, val triggerAtMillis: Long) : AlarmSchedulingResult()
    data class Skipped(val reason: String) : AlarmSchedulingResult()
    data class Failure(val error: Throwable) : AlarmSchedulingResult()
}

interface IntervalTimerAlarmScheduler {
    fun schedule(timer: IntervalTimer): AlarmSchedulingResult
    fun cancel(timerId: Long)
    fun cancelAll(timerIds: List<Long>)
    fun canScheduleExactAlarms(): Boolean
}

@Singleton
class IntervalTimerAlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IntervalTimerAlarmScheduler {

    internal var alarmManagerOverride: AlarmManager? = null
    internal var pendingIntentProvider: ((Context, Long) -> PendingIntent)? = null
    internal var sdkIntOverride: Int? = null

    private val alarmManager: AlarmManager?
        get() = alarmManagerOverride ?: (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)

    private fun getPendingIntent(timerId: Long): PendingIntent {
        return pendingIntentProvider?.invoke(context, timerId)
            ?: IntervalTimerAlarmContract.createPendingIntent(context, timerId)
    }

    override fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        val sdkInt = sdkIntOverride ?: Build.VERSION.SDK_INT
        return if (sdkInt >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun schedule(timer: IntervalTimer): AlarmSchedulingResult {
        if (timer.status != IntervalTimerStatus.RUNNING) {
            cancel(timer.id)
            return AlarmSchedulingResult.Skipped("Timer ${timer.id} is not in RUNNING state (${timer.status}).")
        }

        val triggerAtMillis = timer.nextTriggerAtMillis
        if (triggerAtMillis == null || triggerAtMillis <= 0L) {
            cancel(timer.id)
            return AlarmSchedulingResult.Skipped("Timer ${timer.id} has no valid nextTriggerAtMillis.")
        }

        val manager = alarmManager
            ?: return AlarmSchedulingResult.Failure(IllegalStateException("AlarmManager not available"))

        val pendingIntent = getPendingIntent(timer.id)

        return try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                AlarmSchedulingResult.Success(isExact = true, triggerAtMillis = triggerAtMillis)
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                AlarmSchedulingResult.Success(isExact = false, triggerAtMillis = triggerAtMillis)
            }
        } catch (e: SecurityException) {
            try {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                AlarmSchedulingResult.Success(isExact = false, triggerAtMillis = triggerAtMillis)
            } catch (fallbackError: Throwable) {
                AlarmSchedulingResult.Failure(fallbackError)
            }
        } catch (e: Throwable) {
            AlarmSchedulingResult.Failure(e)
        }
    }

    override fun cancel(timerId: Long) {
        val manager = alarmManager ?: return
        try {
            val pendingIntent = getPendingIntent(timerId)
            manager.cancel(pendingIntent)
        } catch (_: Throwable) {
            // Safe degradation
        }
    }

    override fun cancelAll(timerIds: List<Long>) {
        for (id in timerIds) {
            cancel(id)
        }
    }
}
