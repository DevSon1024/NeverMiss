package com.devson.NeverMiss.core.reminder

import android.content.Context
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.repository.StreakRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import android.app.AlarmManager
import io.mockk.every

class ReminderSchedulerTest {

    private lateinit var scheduler: ReminderScheduler
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<StreakRepository>(relaxed = true)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        scheduler = ReminderScheduler(context, repository)
    }

    @Test
    fun testNextReminder_CompletedToday() {
        val streak = Streak(
            title = "Task",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            reminderTime = "10:00",
            lastCompletedDate = "2026-05-22" // Completed today!
        )

        // Today is May 22nd, 12:00
        val now = LocalDateTime.of(2026, 5, 22, 12, 0)
        val nextReminder = scheduler.calculateNextReminderDateTime(streak, now)

        // Next reminder should rollover to tomorrow May 23rd at 10:00 AM
        val expected = LocalDateTime.of(2026, 5, 23, 10, 0)
        assertEquals(expected, nextReminder)
    }

    @Test
    fun testNextReminder_NotCompleted_BeforeTime() {
        val streak = Streak(
            title = "Task",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            reminderTime = "10:00",
            lastCompletedDate = "2026-05-21" // Last completed yesterday
        )

        // Today is May 22nd, 09:00 AM (before reminder time)
        val now = LocalDateTime.of(2026, 5, 22, 9, 0)
        val nextReminder = scheduler.calculateNextReminderDateTime(streak, now)

        // Next reminder should trigger today May 22nd at 10:00 AM
        val expected = LocalDateTime.of(2026, 5, 22, 10, 0)
        assertEquals(expected, nextReminder)
    }

    @Test
    fun testNextReminder_NotCompleted_AfterTime_WithRepeat() {
        val streak = Streak(
            title = "Task",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            reminderTime = "10:00",
            reminderRepeatHours = 2, // Repeat every 2 hours
            cutoffTime = "22:00",
            lastCompletedDate = "2026-05-21"
        )

        // Today is May 22nd, 11:30 AM (after 10:00 AM)
        val now = LocalDateTime.of(2026, 5, 22, 11, 30)
        val nextReminder = scheduler.calculateNextReminderDateTime(streak, now)

        // Next repeating interval is 10:00 + 2h = 12:00 PM.
        // It is after 11:30 AM and before 10 PM cutoff, so it should fire today at 12:00 PM
        val expected = LocalDateTime.of(2026, 5, 22, 12, 0)
        assertEquals(expected, nextReminder)
    }

    @Test
    fun testNextReminder_NotCompleted_AfterCutoff_WithRepeat() {
        val streak = Streak(
            title = "Task",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            reminderTime = "10:00",
            reminderRepeatHours = 2,
            cutoffTime = "21:00", // 9 PM cutoff
            lastCompletedDate = "2026-05-21"
        )

        // Today is May 22nd, 20:30 (8:30 PM). 
        // Next repeat would be 10:00 AM + 12h = 22:00 (10:00 PM).
        // Since 10:00 PM is past the 9:00 PM cutoff, it should rollover to tomorrow at 10:00 AM
        val now = LocalDateTime.of(2026, 5, 22, 20, 30)
        val nextReminder = scheduler.calculateNextReminderDateTime(streak, now)

        val expected = LocalDateTime.of(2026, 5, 23, 10, 0)
        assertEquals(expected, nextReminder)
    }
}
