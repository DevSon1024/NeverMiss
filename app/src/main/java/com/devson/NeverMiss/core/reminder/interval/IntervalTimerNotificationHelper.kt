package com.devson.NeverMiss.core.reminder.interval

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.devson.NeverMiss.MainActivity
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: IntervalTimerSettingsRepository
) {

    internal var notificationManagerOverride: NotificationManager? = null
    internal var pendingIntentProvider: ((Context, Int, Intent, Int) -> PendingIntent?)? = null
    internal var notificationBuilderOverride: ((IntervalTimer) -> android.app.Notification)? = null

    private val notificationManager: NotificationManager?
        get() = notificationManagerOverride ?: (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)

    companion object {
        const val CHANNEL_INTERVAL_TIMERS_ID = "interval_timer_channel"
        const val NOTIFICATION_ID_BASE = 5_000_000

        const val ACTION_INTERVAL_TIMER_DONE = "com.devson.NeverMiss.ACTION_INTERVAL_TIMER_DONE"
        const val ACTION_INTERVAL_TIMER_SNOOZE = "com.devson.NeverMiss.ACTION_INTERVAL_TIMER_SNOOZE"
        const val ACTION_INTERVAL_TIMER_STOP = "com.devson.NeverMiss.ACTION_INTERVAL_TIMER_STOP"
        const val ACTION_INTERVAL_TIMER_DISMISS = "com.devson.NeverMiss.ACTION_INTERVAL_TIMER_DISMISS"

        const val EXTRA_INTERVAL_TIMER_ID = "com.devson.NeverMiss.EXTRA_INTERVAL_TIMER_ID"
        const val EXTRA_SNOOZE_MILLIS = "com.devson.NeverMiss.EXTRA_SNOOZE_MILLIS"

        fun getNotificationId(timerId: Long): Int {
            return (NOTIFICATION_ID_BASE + (timerId % 1_000_000_000L)).toInt()
        }
    }

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_INTERVAL_TIMERS_ID,
                "Interval & Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and reminders for completed countdowns and repeating interval timers"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(timer: IntervalTimer) {
        val manager = notificationManager ?: return

        val title = when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> "Timer Completed"
            IntervalTimerMode.INTERVAL -> {
                if (timer.status == IntervalTimerStatus.COMPLETED) {
                    "Interval Reminder Completed"
                } else {
                    "Interval Reminder"
                }
            }
        }

        val contentText = when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> {
                "${timer.title} has completed!"
            }
            IntervalTimerMode.INTERVAL -> {
                val totalStr = timer.repeatLimit?.let { " of $it" } ?: ""
                "${timer.title}: occurrence ${timer.completedOccurrences}$totalStr"
            }
        }

        // Tap opens MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_INTERVAL_TIMER_ID, timer.id)
        }
        val contentPendingIntent = pendingIntentProvider?.invoke(
            context,
            getNotificationId(timer.id),
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        ) ?: PendingIntent.getActivity(
            context,
            getNotificationId(timer.id),
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_INTERVAL_TIMERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (timer.mode == IntervalTimerMode.COUNTDOWN) {
            // Dismiss action
            val dismissPendingIntent = createActionPendingIntent(
                timerId = timer.id,
                actionName = ACTION_INTERVAL_TIMER_DISMISS
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
        } else {
            // Repeating interval timer
            if (timer.status == IntervalTimerStatus.COMPLETED) {
                // Completed limit reached
                val dismissPendingIntent = createActionPendingIntent(
                    timerId = timer.id,
                    actionName = ACTION_INTERVAL_TIMER_DISMISS
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Dismiss",
                    dismissPendingIntent
                )
            } else {
                // Still running interval
                val donePendingIntent = createActionPendingIntent(
                    timerId = timer.id,
                    actionName = ACTION_INTERVAL_TIMER_DONE
                )
                val snoozePendingIntent = createActionPendingIntent(
                    timerId = timer.id,
                    actionName = ACTION_INTERVAL_TIMER_SNOOZE
                )
                val stopPendingIntent = createActionPendingIntent(
                    timerId = timer.id,
                    actionName = ACTION_INTERVAL_TIMER_STOP
                )

                builder.addAction(
                    android.R.drawable.ic_menu_save,
                    "Done",
                    donePendingIntent
                )
                builder.addAction(
                    android.R.drawable.ic_menu_recent_history,
                    "Snooze",
                    snoozePendingIntent
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    stopPendingIntent
                )
            }
        }

        try {
            val notification = notificationBuilderOverride?.invoke(timer) ?: builder.build()
            manager.notify(getNotificationId(timer.id), notification)
        } catch (_: SecurityException) {
            // Safely degraded if POST_NOTIFICATIONS was revoked or denied
        } catch (_: Throwable) {
            // Defensive error handling
        }
    }

    fun cancelNotification(timerId: Long) {
        val manager = notificationManager ?: return
        try {
            manager.cancel(getNotificationId(timerId))
        } catch (_: Throwable) {
            // Safe degradation
        }
    }

    private fun createActionPendingIntent(
        timerId: Long,
        actionName: String,
        snoozeMillis: Long = 0L
    ): PendingIntent? {
        val intent = Intent(context, IntervalTimerNotificationActionReceiver::class.java).apply {
            action = "$actionName.$timerId"
            putExtra(EXTRA_INTERVAL_TIMER_ID, timerId)
            if (snoozeMillis > 0L) {
                putExtra(EXTRA_SNOOZE_MILLIS, snoozeMillis)
            }
        }
        val requestCode = (NOTIFICATION_ID_BASE + (timerId * 10) + actionName.hashCode()).toInt()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return pendingIntentProvider?.invoke(context, requestCode, intent, flags)
            ?: PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
            )
    }
}
