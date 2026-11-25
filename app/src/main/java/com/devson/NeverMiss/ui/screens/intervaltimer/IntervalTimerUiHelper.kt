package com.devson.NeverMiss.ui.screens.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus

object IntervalTimerUiHelper {

    fun formatDuration(totalMillis: Long): String {
        val totalSeconds = (totalMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatConfiguredDuration(timer: IntervalTimer): String {
        val totalMillis = when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> timer.durationMillis
            IntervalTimerMode.INTERVAL -> timer.intervalMillis
        }
        val totalSeconds = totalMillis / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")

        val durationStr = parts.joinToString(" ")
        return when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> durationStr
            IntervalTimerMode.INTERVAL -> "Every $durationStr"
        }
    }

    fun formatOccurrences(timer: IntervalTimer): String {
        return if (timer.repeatLimit != null) {
            "${timer.completedOccurrences} / ${timer.repeatLimit}"
        } else {
            "${timer.completedOccurrences} (Unlimited)"
        }
    }
}
