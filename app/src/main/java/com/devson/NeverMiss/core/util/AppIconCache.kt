package com.devson.NeverMiss.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private const val MAX_CACHE_SIZE = 100
    private val cache = LruCache<String, Bitmap>(MAX_CACHE_SIZE)

    fun getAppIconFromCacheOnly(packageName: String?): Bitmap? {
        if (packageName.isNullOrBlank()) return null
        return cache.get(packageName)
    }

    suspend fun getAppIcon(context: Context, packageName: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (packageName.isNullOrBlank()) return@withContext null

        val cached = cache.get(packageName)
        if (cached != null) return@withContext cached

        val packageManager = context.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val drawable = packageManager.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(drawable)
            if (bitmap != null) {
                cache.put(packageName, bitmap)
            }
            bitmap
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun TrackedAppIcon(
    packageName: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    clipShape: Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) {
        mutableStateOf(AppIconCache.getAppIconFromCacheOnly(packageName))
    }

    LaunchedEffect(packageName) {
        if (bitmap == null && !packageName.isNullOrBlank()) {
            bitmap = AppIconCache.getAppIcon(context, packageName)
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(iconSize)
                .clip(clipShape)
        )
    } else {
        Surface(
            shape = clipShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier.size(iconSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize * 0.6f)
                )
            }
        }
    }
}

@Composable
fun AccountIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
    clipShape: Shape = RoundedCornerShape(12.dp)
) {
    Surface(
        shape = clipShape,
        color = backgroundColor,
        modifier = modifier.size(iconSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Account",
                tint = tint,
                modifier = Modifier.size(iconSize * 0.65f)
            )
        }
    }
}
