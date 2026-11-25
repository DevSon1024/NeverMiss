package com.devson.NeverMiss.core.reminder.interval

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.devson.NeverMiss.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IntervalTimerAlertService : Service() {

    @Inject
    lateinit var alertPlayback: IntervalTimerAlertPlayback

    companion object {
        const val ACTION_START_ALERT = "com.devson.NeverMiss.ACTION_START_ALERT"
        const val ACTION_STOP_ALERT = "com.devson.NeverMiss.ACTION_STOP_ALERT"

        const val EXTRA_TIMER_ID = "com.devson.NeverMiss.EXTRA_ALERT_TIMER_ID"
        const val EXTRA_TIMER_TITLE = "com.devson.NeverMiss.EXTRA_ALERT_TIMER_TITLE"

        const val ALERT_NOTIFICATION_ID = 5_999_999

        fun start(context: Context, timerId: Long, timerTitle: String) {
            val intent = Intent(context, IntervalTimerAlertService::class.java).apply {
                action = ACTION_START_ALERT
                putExtra(EXTRA_TIMER_ID, timerId)
                putExtra(EXTRA_TIMER_TITLE, timerTitle)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
                // Background start restriction fallback: notification is already posted
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IntervalTimerAlertService::class.java).apply {
                action = ACTION_STOP_ALERT
            }
            try {
                context.startService(intent)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ALERT) {
            stopAlertAndSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_ALERT) {
            val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
            val timerTitle = intent.getStringExtra(EXTRA_TIMER_TITLE) ?: "Interval Timer"

            // 1. Immediately call startForeground with persistent alert notification
            val notification = createForegroundNotification(timerTitle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ALERT_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(ALERT_NOTIFICATION_ID, notification)
            }

            // 2. Start audio and vibration playback
            alertPlayback.startAlert(durationMillis = 30_000L)
        }

        return START_NOT_STICKY
    }

    private fun stopAlertAndSelf() {
        alertPlayback.stopAlert()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        alertPlayback.stopAlert()
        super.onDestroy()
    }

    private fun createForegroundNotification(title: String): Notification {
        return NotificationCompat.Builder(this, IntervalTimerNotificationHelper.CHANNEL_INTERVAL_TIMERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Timer Ringing")
            .setContentText("$title alert active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
}
