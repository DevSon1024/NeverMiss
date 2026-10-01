package com.devson.NeverMiss.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Streak Daily Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up notifications to log your daily streaks"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(streak: Streak, customMessage: String? = null) {
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

        val message = customMessage ?: "Time to complete your routine: ${streak.title}!"

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${streak.emoji} ${streak.title}")
            .setContentText(message)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Quick Action Buttons
        val doneIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_DONE)
        val snoozeIntent = createActionIntent(streak.id, NotificationActionReceiver.ACTION_SNOOZE)

        builder.addAction(
            android.R.drawable.ic_menu_save,
            "Done",
            doneIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_recent_history,
            "Snooze 10m",
            snoozeIntent
        )

        notificationManager.notify(streak.id.toInt(), builder.build())
    }

    fun cancelNotification(streakId: Long) {
        notificationManager.cancel(streakId.toInt())
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
