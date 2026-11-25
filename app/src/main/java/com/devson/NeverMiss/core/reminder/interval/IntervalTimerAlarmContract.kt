package com.devson.NeverMiss.core.reminder.interval

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object IntervalTimerAlarmContract {

    const val ACTION_INTERVAL_TIMER_ALARM = "com.devson.NeverMiss.ACTION_INTERVAL_TIMER_ALARM"
    const val EXTRA_TIMER_ID = "com.devson.NeverMiss.EXTRA_TIMER_ID"
    const val URI_SCHEME = "nevermiss"
    const val URI_HOST = "interval_timer"

    fun getAction(timerId: Long): String = "$ACTION_INTERVAL_TIMER_ALARM.$timerId"

    fun getDataUriString(timerId: Long): String = "$URI_SCHEME://$URI_HOST/$timerId"

    fun createIntent(context: Context, timerId: Long): Intent {
        return Intent(context, IntervalTimerAlarmReceiver::class.java).apply {
            action = getAction(timerId)
            data = Uri.parse(getDataUriString(timerId))
            putExtra(EXTRA_TIMER_ID, timerId)
        }
    }

    fun getRequestCode(timerId: Long): Int {
        // Offset by 1_000_000 to guarantee identity separation from streak reminder request codes
        return (1_000_000L + (timerId % 1_000_000_000L)).toInt()
    }

    fun createPendingIntent(
        context: Context,
        timerId: Long,
        flags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    ): PendingIntent {
        val intent = createIntent(context, timerId)
        return PendingIntent.getBroadcast(
            context,
            getRequestCode(timerId),
            intent,
            flags
        )
    }
}
