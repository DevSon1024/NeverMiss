package com.devson.NeverMiss.data.local.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getInstalledLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val selfPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList = try {
            packageManager.queryIntentActivities(mainIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val seenPackages = mutableSetOf<String>()
        val result = mutableListOf<InstalledAppInfo>()

        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo?.packageName ?: continue
            if (packageName == selfPackage || seenPackages.contains(packageName)) {
                continue
            }
            seenPackages.add(packageName)

            val appName = try {
                resolveInfo.loadLabel(packageManager).toString().ifBlank { packageName }
            } catch (e: Exception) {
                packageName
            }

            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystemApp = if (appInfo != null) {
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            } else false

            result.add(
                InstalledAppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystemApp,
                    isLaunchable = true
                )
            )
        }

        result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    suspend fun getAppDetails(packageName: String): InstalledAppInfo? = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            InstalledAppInfo(
                packageName = packageName,
                appName = appName,
                icon = icon,
                isSystemApp = isSystemApp,
                isLaunchable = true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

