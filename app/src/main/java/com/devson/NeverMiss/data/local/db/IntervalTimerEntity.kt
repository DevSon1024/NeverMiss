package com.devson.NeverMiss.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interval_timers",
    indices = [
        Index(value = ["status"]),
        Index(value = ["nextTriggerAtMillis"])
    ]
)
data class IntervalTimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val mode: String,
    val durationMillis: Long,
    val intervalMillis: Long,
    val repeatLimit: Int? = null,
    val completedOccurrences: Int = 0,
    val status: String,
    val startedAtMillis: Long? = null,
    val nextTriggerAtMillis: Long? = null,
    val remainingAtPauseMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
