package com.devson.NeverMiss.core.reminder.interval

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertPlayback: IntervalTimerAlertPlayback
) {

    fun startAlert(timerId: Long, timerTitle: String) {
        IntervalTimerAlertService.start(context, timerId, timerTitle)
    }

    fun stopAlert() {
        IntervalTimerAlertService.stop(context)
        alertPlayback.stopAlert()
    }
}
