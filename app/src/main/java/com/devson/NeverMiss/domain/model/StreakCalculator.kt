package com.devson.NeverMiss.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StreakCalculator {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: Exception) {
            com.devson.NeverMiss.core.util.AppClock.now()
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun isScheduled(streak: Streak, date: LocalDate): Boolean {
        val creation = parseDate(streak.creationDate)
        if (date.isBefore(creation)) return false

        return if (streak.daysOfWeek.isNotEmpty()) {
            // LocalDate dayOfWeek: Monday=1, Sunday=7
            val dayVal = date.dayOfWeek.value
            streak.daysOfWeek.contains(dayVal)
        } else {
            val daysBetween = ChronoUnit.DAYS.between(creation, date)
            daysBetween % streak.repeatIntervalDays == 0L
        }
    }

    /**
     * Checks if the cutoff has passed for a given date.
     * Cutoff time is HH:mm. If gracePeriodUntil3AM is true, the cutoff is 03:00 AM of the NEXT day.
     */
    fun hasCutoffPassed(streak: Streak, date: LocalDate, nowTime: LocalTime = com.devson.NeverMiss.core.util.AppClock.nowTime(), nowDate: LocalDate = com.devson.NeverMiss.core.util.AppClock.now()): Boolean {
        val cutoffLocalTime = try {
            LocalTime.parse(streak.cutoffTime)
        } catch (e: Exception) {
            LocalTime.MIDNIGHT
        }

        return if (streak.gracePeriodUntil3AM) {
            // Cutoff is 3 AM of the next day.
            // If checking for today, cutoff is tomorrow at 3 AM.
            val cutoffDateTime = date.plusDays(1).atTime(LocalTime.of(3, 0))
            val currentDateTime = nowDate.atTime(nowTime)
            currentDateTime.isAfter(cutoffDateTime)
        } else {
            // If cutoff is 12 AM (midnight/00:00), the deadline is midnight of the next day.
            val cutoffDateTime = if (cutoffLocalTime == LocalTime.MIDNIGHT) {
                date.plusDays(1).atTime(LocalTime.MIDNIGHT)
            } else {
                date.atTime(cutoffLocalTime)
            }
            val currentDateTime = nowDate.atTime(nowTime)
            currentDateTime.isAfter(cutoffDateTime)
        }
    }

    fun calculateStreaks(
        streak: Streak,
        history: List<CompletionHistory>,
        nowDate: LocalDate = com.devson.NeverMiss.core.util.AppClock.now(),
        nowTime: LocalTime = com.devson.NeverMiss.core.util.AppClock.nowTime()
    ): StreakStats {
        val historyMap = history.associateBy { it.date }
        val creationLocalDate = parseDate(streak.creationDate)
        
        if (nowDate.isBefore(creationLocalDate)) {
            return StreakStats(0, 0)
        }

        // 1. Calculate Current Streak
        var current = 0
        var checkDate = nowDate
        var isStreakBroken = false

        while (!checkDate.isBefore(creationLocalDate)) {
            val isDayScheduled = isScheduled(streak, checkDate)
            if (isDayScheduled) {
                val status = historyMap[formatDate(checkDate)]?.status
                when (status) {
                    CompletionStatus.COMPLETED -> {
                        current++
                    }
                    CompletionStatus.SKIPPED, CompletionStatus.FROZEN -> {
                        // Keeps streak alive but doesn't increment the counter
                    }
                    null -> {
                        // Missed completion
                        if (checkDate == nowDate) {
                            // If today, check if cutoff has passed
                            if (hasCutoffPassed(streak, nowDate, nowTime, nowDate)) {
                                isStreakBroken = true
                            }
                        } else {
                            // Past day scheduled but not completed -> breaks current streak
                            isStreakBroken = true
                        }
                    }
                }
            }

            if (isStreakBroken) {
                break
            }
            checkDate = checkDate.minusDays(1)
        }

        // 2. Calculate Longest Streak (Chronological traversal)
        var maxStreak = 0
        var runningStreak = 0
        val totalDays = ChronoUnit.DAYS.between(creationLocalDate, nowDate)
        
        for (i in 0..totalDays) {
            val date = creationLocalDate.plusDays(i)
            if (isScheduled(streak, date)) {
                val status = historyMap[formatDate(date)]?.status
                when (status) {
                    CompletionStatus.COMPLETED -> {
                        runningStreak++
                        if (runningStreak > maxStreak) {
                            maxStreak = runningStreak
                        }
                    }
                    CompletionStatus.SKIPPED, CompletionStatus.FROZEN -> {
                        // Keeps running streak alive but doesn't increment
                    }
                    null -> {
                        // Breaks running streak if it is in the past
                        // or if it's today and today's cutoff passed
                        if (date < nowDate || hasCutoffPassed(streak, date, nowTime, nowDate)) {
                            runningStreak = 0
                        }
                    }
                }
            }
        }

        return StreakStats(current, maxStreak)
    }

    data class StreakStats(val currentStreak: Int, val longestStreak: Int)
}
