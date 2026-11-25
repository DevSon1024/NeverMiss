package com.devson.NeverMiss.core.reminder.interval

import com.devson.NeverMiss.core.util.TimeProvider
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import com.devson.NeverMiss.domain.usecase.intervaltimer.AdvanceIntervalTimerOccurrenceUseCase
import com.devson.NeverMiss.domain.usecase.intervaltimer.CompleteIntervalTimerUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerAlarmProcessor @Inject constructor(
    private val repository: IntervalTimerRepository,
    private val advanceOccurrenceUseCase: AdvanceIntervalTimerOccurrenceUseCase,
    private val completeTimerUseCase: CompleteIntervalTimerUseCase,
    private val notificationHelper: IntervalTimerNotificationHelper,
    private val alertManager: IntervalTimerAlertManager,
    private val timeProvider: TimeProvider
) {
    private val mutexMap = ConcurrentHashMap<Long, Mutex>()

    suspend fun processDueAlarm(timerId: Long, allowImmediateIfDue: Boolean = false) {
        val mutex = mutexMap.computeIfAbsent(timerId) { Mutex() }
        mutex.withLock {
            val now = timeProvider.currentTimeMillis()

            // 1. Fetch latest timer state from repository
            val timer = repository.getTimerById(timerId) ?: return

            // 2. Validate timer status
            if (timer.status != IntervalTimerStatus.RUNNING) {
                return
            }

            // 3. Validate trigger timestamp
            val scheduledTrigger = timer.nextTriggerAtMillis ?: return
            // Allow a small 500ms tolerance for clock jitter, but reject clearly premature triggers
            if (!allowImmediateIfDue && now < scheduledTrigger - 500L) {
                return
            }

            // 4. Process occurrence based on mode
            when (timer.mode) {
                IntervalTimerMode.COUNTDOWN -> {
                    // Complete the countdown timer
                    val completed = completeTimerUseCase(timerId, now)
                    if (completed) {
                        val updatedTimer = repository.getTimerById(timerId)
                        if (updatedTimer != null && updatedTimer.status == IntervalTimerStatus.COMPLETED) {
                            notificationHelper.showTimerNotification(updatedTimer)
                            alertManager.startAlert(updatedTimer.id, updatedTimer.title)
                        }
                    }
                }
                IntervalTimerMode.INTERVAL -> {
                    // Advance repeating occurrence
                    val advanced = advanceOccurrenceUseCase(timerId, now)
                    if (advanced) {
                        val updatedTimer = repository.getTimerById(timerId)
                        if (updatedTimer != null) {
                            notificationHelper.showTimerNotification(updatedTimer)
                            alertManager.startAlert(updatedTimer.id, updatedTimer.title)
                        }
                    }
                }
            }
        }
    }
}
