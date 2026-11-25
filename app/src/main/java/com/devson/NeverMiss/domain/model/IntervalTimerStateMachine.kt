package com.devson.NeverMiss.domain.model

object IntervalTimerStateMachine {

    fun start(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.status != IntervalTimerStatus.IDLE &&
            timer.status != IntervalTimerStatus.STOPPED &&
            timer.status != IntervalTimerStatus.COMPLETED
        ) {
            throw IllegalStateException(
                "Cannot start timer in ${timer.status} state. Only IDLE, STOPPED, or COMPLETED timers can be started."
            )
        }

        val targetDuration = when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> timer.durationMillis
            IntervalTimerMode.INTERVAL -> timer.intervalMillis
        }
        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)

        val updated = timer.copy(
            status = IntervalTimerStatus.RUNNING,
            startedAtMillis = nowMillis,
            nextTriggerAtMillis = nowMillis + targetDuration,
            remainingAtPauseMillis = null,
            completedOccurrences = 0,
            updatedAtMillis = safeUpdatedAt
        )
        updated.validate()
        return updated
    }

    fun pause(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.status != IntervalTimerStatus.RUNNING) {
            throw IllegalStateException(
                "Cannot pause timer in ${timer.status} state. Only RUNNING timers can be paused."
            )
        }

        val triggerAt = timer.nextTriggerAtMillis ?: (nowMillis + if (timer.mode == IntervalTimerMode.COUNTDOWN) timer.durationMillis else timer.intervalMillis)
        val remaining = (triggerAt - nowMillis).coerceAtLeast(1L)
        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)

        val updated = timer.copy(
            status = IntervalTimerStatus.PAUSED,
            nextTriggerAtMillis = null,
            remainingAtPauseMillis = remaining,
            updatedAtMillis = safeUpdatedAt
        )
        updated.validate()
        return updated
    }

    fun resume(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.status != IntervalTimerStatus.PAUSED) {
            throw IllegalStateException(
                "Cannot resume timer in ${timer.status} state. Only PAUSED timers can be resumed."
            )
        }

        val remaining = requireNotNull(timer.remainingAtPauseMillis) {
            "Cannot resume timer without remainingAtPauseMillis."
        }
        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)

        val updated = timer.copy(
            status = IntervalTimerStatus.RUNNING,
            nextTriggerAtMillis = nowMillis + remaining,
            remainingAtPauseMillis = null,
            updatedAtMillis = safeUpdatedAt
        )
        updated.validate()
        return updated
    }

    fun stop(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.status == IntervalTimerStatus.STOPPED) {
            return timer
        }
        if (timer.status != IntervalTimerStatus.RUNNING && timer.status != IntervalTimerStatus.PAUSED) {
            throw IllegalStateException(
                "Cannot stop timer in ${timer.status} state. Only RUNNING or PAUSED timers can be stopped."
            )
        }

        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)
        val updated = timer.copy(
            status = IntervalTimerStatus.STOPPED,
            nextTriggerAtMillis = null,
            remainingAtPauseMillis = null,
            updatedAtMillis = safeUpdatedAt
        )
        updated.validate()
        return updated
    }

    fun complete(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.status == IntervalTimerStatus.COMPLETED) {
            return timer
        }
        if (timer.status != IntervalTimerStatus.RUNNING) {
            throw IllegalStateException(
                "Cannot complete timer in ${timer.status} state. Only RUNNING timers can be completed."
            )
        }

        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)
        val targetOccurrences = when (timer.mode) {
            IntervalTimerMode.COUNTDOWN -> 1
            IntervalTimerMode.INTERVAL -> timer.repeatLimit ?: (timer.completedOccurrences + 1)
        }

        val updated = timer.copy(
            status = IntervalTimerStatus.COMPLETED,
            nextTriggerAtMillis = null,
            remainingAtPauseMillis = null,
            completedOccurrences = targetOccurrences,
            updatedAtMillis = safeUpdatedAt
        )
        updated.validate()
        return updated
    }

    fun advanceOccurrence(timer: IntervalTimer, nowMillis: Long): IntervalTimer {
        if (timer.mode != IntervalTimerMode.INTERVAL) {
            throw IllegalStateException(
                "Cannot advance occurrence for a COUNTDOWN timer. Use complete() instead."
            )
        }
        if (timer.status != IntervalTimerStatus.RUNNING) {
            throw IllegalStateException(
                "Cannot advance occurrence for timer in ${timer.status} state. Timer must be RUNNING."
            )
        }

        val scheduledTrigger = requireNotNull(timer.nextTriggerAtMillis) {
            "RUNNING interval timer must have a non-null nextTriggerAtMillis."
        }
        if (nowMillis < scheduledTrigger) {
            throw IllegalStateException(
                "Cannot advance occurrence before nextTriggerAtMillis has arrived (nowMillis: $nowMillis, nextTriggerAtMillis: $scheduledTrigger)."
            )
        }

        val nextCompleted = timer.completedOccurrences + 1
        val safeUpdatedAt = maxOf(nowMillis, timer.createdAtMillis)

        val isCompleted = timer.repeatLimit != null && nextCompleted >= timer.repeatLimit
        if (isCompleted) {
            val completedTimer = timer.copy(
                status = IntervalTimerStatus.COMPLETED,
                completedOccurrences = nextCompleted,
                nextTriggerAtMillis = null,
                remainingAtPauseMillis = null,
                updatedAtMillis = safeUpdatedAt
            )
            completedTimer.validate()
            return completedTimer
        }

        // Late trigger strategy:
        // If nowMillis is delayed by a full interval or more beyond the scheduled trigger,
        // calculate the next occurrence from nowMillis to avoid catch-up trigger storms.
        // Otherwise, maintain scheduled cadence.
        val nextOccurrenceTrigger = if (nowMillis >= scheduledTrigger + timer.intervalMillis) {
            nowMillis + timer.intervalMillis
        } else {
            scheduledTrigger + timer.intervalMillis
        }

        val advancedTimer = timer.copy(
            status = IntervalTimerStatus.RUNNING,
            completedOccurrences = nextCompleted,
            nextTriggerAtMillis = nextOccurrenceTrigger,
            remainingAtPauseMillis = null,
            updatedAtMillis = safeUpdatedAt
        )
        advancedTimer.validate()
        return advancedTimer
    }

    fun calculateRemainingMillis(timer: IntervalTimer, nowMillis: Long): Long {
        return timer.remainingMillis(nowMillis)
    }

    fun calculateNextTriggerMillis(timer: IntervalTimer, nowMillis: Long): Long? {
        return when (timer.status) {
            IntervalTimerStatus.RUNNING -> timer.nextTriggerAtMillis
            IntervalTimerStatus.PAUSED -> nowMillis + (timer.remainingAtPauseMillis ?: 0L)
            IntervalTimerStatus.IDLE,
            IntervalTimerStatus.STOPPED,
            IntervalTimerStatus.COMPLETED -> null
        }
    }
}
