package com.devson.NeverMiss.data.local.apps

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val isLaunchable: Boolean = true,
    val isTracked: Boolean = false,
    val trackedAppId: Long? = null,
    val isTrackingEnabled: Boolean = false
)

