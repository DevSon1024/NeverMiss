package com.devson.NeverMiss.core.reminder.interval

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_APPLICABLE
}

data class IntervalTimerPermissionsSnapshot(
    val notificationStatus: PermissionStatus,
    val exactAlarmStatus: PermissionStatus,
    val canScheduleExactAlarms: Boolean
)

@Singleton
class IntervalTimerPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun checkNotificationPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        } else {
            PermissionStatus.NOT_APPLICABLE
        }
    }

    fun checkExactAlarmPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val canExact = alarmManager?.canScheduleExactAlarms() ?: false
            if (canExact) PermissionStatus.GRANTED else PermissionStatus.DENIED
        } else {
            PermissionStatus.NOT_APPLICABLE
        }
    }

    fun getPermissionsSnapshot(): IntervalTimerPermissionsSnapshot {
        val notif = checkNotificationPermission()
        val exact = checkExactAlarmPermission()
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exact == PermissionStatus.GRANTED
        } else {
            true
        }
        return IntervalTimerPermissionsSnapshot(
            notificationStatus = notif,
            exactAlarmStatus = exact,
            canScheduleExactAlarms = canExact
        )
    }

    fun createNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            createApplicationDetailsSettingsIntent()
        }
    }

    fun createExactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            createApplicationDetailsSettingsIntent()
        }
    }

    fun createApplicationDetailsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
