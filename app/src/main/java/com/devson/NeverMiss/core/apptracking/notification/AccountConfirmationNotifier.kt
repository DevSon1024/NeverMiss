package com.devson.NeverMiss.core.apptracking.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.devson.NeverMiss.MainActivity
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountConfirmationNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_CONFIRMATIONS_ID = "app_account_confirmations"
        const val ACTION_CONFIRM_ACCOUNT = "com.devson.NeverMiss.ACTION_CONFIRM_ACCOUNT"
        const val EXTRA_CONFIRMATION_ID = "extra_confirmation_id"
        const val EXTRA_APP_ID = "extra_app_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_CONFIRMATIONS_ID,
                "App Account Confirmations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to log sub-streaks for your accounts"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyPendingConfirmation(
        confirmation: PendingConfirmation,
        app: TrackedApp,
        accounts: List<TrackedAccount>
    ) {
        if (notificationManager == null) return

        // Respect Android 13+ POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val accountNames = accounts.joinToString(", ") { it.accountName }
        val title = "Which account did you use on ${app.appName}?"
        val contentText = if (accounts.isNotEmpty()) {
            "Tap to log your sub-streak for $accountNames"
        } else {
            "Tap to confirm your session on ${app.appName}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CONFIRM_ACCOUNT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CONFIRMATION_ID, confirmation.id)
            putExtra(EXTRA_APP_ID, app.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            confirmation.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CONFIRMATIONS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(confirmation.id.toInt(), builder.build())
    }

    fun cancelNotification(confirmationId: Long) {
        notificationManager?.cancel(confirmationId.toInt())
    }
}

