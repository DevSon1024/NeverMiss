package com.devson.NeverMiss.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

object AppClock {
    @Volatile
    private var customDate: LocalDate? = null
    @Volatile
    private var isOverrideEnabled: Boolean = false

    fun init(overrideEnabled: Boolean, dateStr: String) {
        isOverrideEnabled = overrideEnabled
        customDate = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    fun setOverride(enabled: Boolean, date: LocalDate?) {
        isOverrideEnabled = enabled
        customDate = date
    }

    fun now(): LocalDate {
        val date = customDate
        return if (isOverrideEnabled && date != null) {
            date
        } else {
            LocalDate.now()
        }
    }

    fun nowTime(): LocalTime {
        return LocalTime.now()
    }

    fun nowDateTime(): LocalDateTime {
        val date = customDate
        return if (isOverrideEnabled && date != null) {
            date.atTime(LocalTime.now())
        } else {
            LocalDateTime.now()
        }
    }

    fun isOverrideActive(): Boolean {
        return isOverrideEnabled && customDate != null
    }
}
