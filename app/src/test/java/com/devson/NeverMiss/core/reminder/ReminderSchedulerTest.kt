package com.devson.NeverMiss.core.reminder

import android.app.AlarmManager
import android.content.Context
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.repository.StreakRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

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
            lastCompletedDate = "2026-05-22" // Completed today
        )

        // Today is May 22nd, 12:00 PM
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
    fun testNextReminder_NotCompleted_AfterTime() {
        val streak = Streak(
            title = "Task",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            reminderTime = "10:00",
            lastCompletedDate = "2026-05-21"
        )

        // Today is May 22nd, 11:30 AM (after 10:00 AM reminder time)
        val now = LocalDateTime.of(2026, 5, 22, 11, 30)
        val nextReminder = scheduler.calculateNextReminderDateTime(streak, now)

        // It is past 10:00 AM today, so it should rollover to tomorrow May 23rd at 10:00 AM
        val expected = LocalDateTime.of(2026, 5, 23, 10, 0)
        assertEquals(expected, nextReminder)
    }
}
