package com.devson.NeverMiss.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.devson.NeverMiss.MainActivity
import com.devson.NeverMiss.domain.model.Streak
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_REMINDERS_ID = "streak_reminders"
        const val CHANNEL_DANGER_ID = "danger_reminders"
        const val CHANNEL_PERSISTENT_ID = "persistent_reminders"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Standard Reminders
            val standardChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Streak Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard reminders to complete your active streaks"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }

            // Channel 2: Danger Warnings
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            
            val dangerChannel = NotificationChannel(
                CHANNEL_DANGER_ID,
                "Urgent Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts before your streak is about to break"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setSound(alarmSound, audioAttributes)
            }

            // Channel 3: Persistent Notifications
            val persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT_ID,
                "Ongoing Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pinned reminders that stay in the notification tray until marked done"
            }

            notificationManager.createNotificationChannel(standardChannel)
            notificationManager.createNotificationChannel(dangerChannel)
            notificationManager.createNotificationChannel(persistentChannel)
        }
    }

    fun showReminderNotification(streak: Streak, message: String) {
        val channelId = if (streak.persistent) CHANNEL_PERSISTENT_ID else CHANNEL_REMINDERS_ID
        val builder = buildNotification(streak, message, channelId)
        
        if (streak.persistent) {
            builder.setOngoing(true)
        }

        notificationManager.notify(streak.id.toInt(), builder.build())
    }

    fun showDangerNotification(streak: Streak, message: String) {
        val builder = buildNotification(streak, message, CHANNEL_DANGER_ID)
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        notificationManager.notify(streak.id.toInt() + 100000, builder.build())
    }

    fun cancelNotification(streakId: Long) {
        notificationManager.cancel(streakId.toInt())
        notificationManager.cancel(streakId.toInt() + 100000)
    }

    private fun buildNotification(
        streak: Streak,
        message: String,
        channelId: String
    ): NotificationCompat.Builder {
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("STREAK_ID", streak.id)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            streak.id.toInt() + 200,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = streak.notificationSound?.let { Uri.parse(it) } 
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System icon fallback
            .setContentTitle("${streak.emoji} ${streak.title}")
            .setContentText(message)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(!streak.persistent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)

        if (streak.vibrate) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        } else {
            builder.setVibrate(null)
        }

        // Add Quick Action Buttons
        val doneIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_DONE)
        val snoozeIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_SNOOZE)
        val skipIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_SKIP)
        val freezeIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_FREEZE)

        builder.addAction(
            android.R.drawable.ic_menu_save,
            "Done",
            doneIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_recent_history,
            "Snooze",
            snoozeIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Skip",
            skipIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_gallery,
            "Freeze",
            freezeIntent
        )

        return builder
    }

    private fun createActionIntent(streakId: Long, actionName: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionName
            putExtra("STREAK_ID", streakId)
        }
        val requestCode = (streakId.toInt() * 10) + actionName.hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
