package com.devson.NeverMiss.domain.usecase.appstreaks

import android.content.Context
import com.devson.NeverMiss.core.apptracking.permission.UsageAccessPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckUsagePermissionUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean {
        return UsageAccessPermissionHelper.hasUsageAccessPermission(context)
    }
}

