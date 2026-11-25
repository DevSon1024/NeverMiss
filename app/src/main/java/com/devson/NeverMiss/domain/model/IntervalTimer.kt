package com.devson.NeverMiss.domain.model

enum class IntervalTimerMode {
    COUNTDOWN,
    INTERVAL
}

enum class IntervalTimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    STOPPED
}

data class IntervalTimer(
    val id: Long = 0L,
    val title: String,
    val mode: IntervalTimerMode,
    val durationMillis: Long = 0L,
    val intervalMillis: Long = 0L,
    val repeatLimit: Int? = null,
    val completedOccurrences: Int = 0,
    val status: IntervalTimerStatus = IntervalTimerStatus.IDLE,
    val startedAtMillis: Long? = null,
    val nextTriggerAtMillis: Long? = null,
    val remainingAtPauseMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis
) {
    init {
        validate()
    }

    fun validate() {
        require(id >= 0L) { "Timer id must be non-negative." }
        require(title.isNotBlank()) { "Timer title must not be blank." }
        when (mode) {
            IntervalTimerMode.COUNTDOWN -> {
                require(durationMillis > 0L) { "Countdown duration must be positive." }
                require(intervalMillis >= 0L) { "Interval duration must not be negative." }
            }
            IntervalTimerMode.INTERVAL -> {
                require(intervalMillis > 0L) { "Interval duration must be positive." }
                require(durationMillis >= 0L) { "Countdown duration must not be negative." }
            }
        }
        require(repeatLimit == null || repeatLimit >= 0) { "Repeat limit must not be negative." }
        require(completedOccurrences >= 0) { "Completed occurrences must not be negative." }
        if (repeatLimit != null) {
            require(completedOccurrences <= repeatLimit) {
                "Completed occurrences ($completedOccurrences) cannot exceed repeat limit ($repeatLimit)."
            }
        }
        require(createdAtMillis >= 0L) { "Creation timestamp must be non-negative." }
        require(updatedAtMillis >= createdAtMillis) {
            "Updated timestamp must not precede creation timestamp."
        }

        when (status) {
            IntervalTimerStatus.IDLE -> {
                require(startedAtMillis == null) { "IDLE timer cannot have startedAtMillis." }
                require(nextTriggerAtMillis == null) { "IDLE timer cannot have nextTriggerAtMillis." }
                require(remainingAtPauseMillis == null) { "IDLE timer cannot have remainingAtPauseMillis." }
            }
            IntervalTimerStatus.RUNNING -> {
                require(startedAtMillis != null && startedAtMillis >= 0L) {
                    "RUNNING timer must have a valid startedAtMillis."
                }
                require(nextTriggerAtMillis != null && nextTriggerAtMillis >= startedAtMillis) {
                    "RUNNING timer must have nextTriggerAtMillis >= startedAtMillis."
                }
                require(remainingAtPauseMillis == null) {
                    "RUNNING timer cannot have remainingAtPauseMillis."
                }
            }
            IntervalTimerStatus.PAUSED -> {
                require(remainingAtPauseMillis != null && remainingAtPauseMillis > 0L) {
                    "PAUSED timer must have positive remainingAtPauseMillis."
                }
                require(nextTriggerAtMillis == null) {
                    "PAUSED timer cannot have nextTriggerAtMillis."
                }
            }
            IntervalTimerStatus.COMPLETED,
            IntervalTimerStatus.STOPPED -> {
                require(nextTriggerAtMillis == null) {
                    "$status timer cannot have nextTriggerAtMillis."
                }
                require(remainingAtPauseMillis == null) {
                    "$status timer cannot have remainingAtPauseMillis."
                }
            }
        }
    }

    fun remainingMillis(nowMillis: Long): Long {
        return when (status) {
            IntervalTimerStatus.IDLE -> if (mode == IntervalTimerMode.COUNTDOWN) durationMillis else intervalMillis
            IntervalTimerStatus.RUNNING -> ((nextTriggerAtMillis ?: nowMillis) - nowMillis).coerceAtLeast(0L)
            IntervalTimerStatus.PAUSED -> (remainingAtPauseMillis ?: 0L).coerceAtLeast(0L)
            IntervalTimerStatus.COMPLETED,
            IntervalTimerStatus.STOPPED -> 0L
        }
    }
}
