package com.devson.NeverMiss.core.apptracking.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsageAccessPermissionHelperTest {

    private val context = mockk<Context>(relaxed = true)
    private val appOps = mockk<AppOpsManager>(relaxed = true)
    private val packageManager = mockk<PackageManager>(relaxed = true)
    private val mockUri = mockk<Uri>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(Process::class)

        every { Process.myUid() } returns 10001
        every { Uri.fromParts("package", any(), null) } returns mockUri
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOps
        every { context.packageName } returns "com.devson.NeverMiss"
        every { context.packageManager } returns packageManager
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(Process::class)
    }

    @Test
    fun testHasUsageAccessPermission_allowed_returnsTrue() {
        @Suppress("DEPRECATION")
        every {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_ALLOWED
        @Suppress("DEPRECATION")
        every {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_ALLOWED

        val hasPermission = UsageAccessPermissionHelper.hasUsageAccessPermission(context)
        assertTrue(hasPermission)
    }

    @Test
    fun testHasUsageAccessPermission_ignored_returnsFalse() {
        @Suppress("DEPRECATION")
        every {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_IGNORED
        @Suppress("DEPRECATION")
        every {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_IGNORED

        val hasPermission = UsageAccessPermissionHelper.hasUsageAccessPermission(context)
        assertFalse(hasPermission)
    }

    @Test
    fun testHasUsageAccessPermission_nullAppOps_returnsFalse() {
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns null

        val hasPermission = UsageAccessPermissionHelper.hasUsageAccessPermission(context)
        assertFalse(hasPermission)
    }

    @Test
    fun testCreateUsageAccessIntent_returnsValidSettingsIntent() {
        every { packageManager.resolveActivity(any(), any<Int>()) } returns null

        val intent = UsageAccessPermissionHelper.createUsageAccessIntent(context)
        assertNotNull(intent)
    }
}
