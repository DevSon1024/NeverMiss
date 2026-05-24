package com.devson.NeverMiss.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.usecase.StreakUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateStreakState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val emoji: String = "✨",
    val category: StreakCategory = StreakCategory.CUSTOM,
    val color: Int = 0xFF6200EE.toInt(), // Default Purple
    val creationDate: String = "", // yyyy-MM-dd
    val repeatIntervalDays: Int = 1,
    val daysOfWeek: List<Int> = emptyList(), // 1=Mon, 7=Sun
    val reminderTime: String = "10:00", // HH:mm
    val reminderRepeatHours: Int = 0,
    val remindUntilDone: Boolean = false,
    val dangerReminderMinutes: Int = 0,
    val cutoffTime: String = "00:00", // HH:mm
    val gracePeriodUntil3AM: Boolean = false,
    val notificationSound: String? = null,
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val persistent: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateStreakViewModel @Inject constructor(
    private val streakUseCases: StreakUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(CreateStreakState())
    val state: StateFlow<CreateStreakState> = _state.asStateFlow()

    fun loadStreak(streakId: Long) {
        if (streakId <= 0) return
        viewModelScope.launch {
            val streak = streakUseCases.getStreakById(streakId).first()
            if (streak != null) {
                _state.update {
                    CreateStreakState(
                        id = streak.id,
                        title = streak.title,
                        description = streak.description,
                        emoji = streak.emoji,
                        category = streak.category,
                        color = streak.color,
                        creationDate = streak.creationDate,
                        repeatIntervalDays = streak.repeatIntervalDays,
                        daysOfWeek = streak.daysOfWeek,
                        reminderTime = streak.reminderTime,
                        reminderRepeatHours = streak.reminderRepeatHours,
                        remindUntilDone = streak.remindUntilDone,
                        dangerReminderMinutes = streak.dangerReminderMinutes,
                        cutoffTime = streak.cutoffTime,
                        gracePeriodUntil3AM = streak.gracePeriodUntil3AM,
                        notificationSound = streak.notificationSound,
                        vibrate = streak.vibrate,
                        silent = streak.silent,
                        persistent = streak.persistent
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title, errorMessage = null) }
    }

    fun onDescriptionChange(desc: String) {
        _state.update { it.copy(description = desc) }
    }

    fun onEmojiChange(emoji: String) {
        _state.update { it.copy(emoji = emoji) }
    }

    fun onCategoryChange(category: StreakCategory) {
        _state.update { it.copy(category = category, emoji = category.defaultEmoji) }
    }

    fun onColorChange(color: Int) {
        _state.update { it.copy(color = color) }
    }

    fun onIntervalChange(days: Int) {
        _state.update { it.copy(repeatIntervalDays = days, daysOfWeek = emptyList()) }
    }

    fun toggleDayOfWeek(day: Int) {
        _state.update { currentState ->
            val list = currentState.daysOfWeek.toMutableList()
            if (list.contains(day)) {
                list.remove(day)
            } else {
                list.add(day)
            }
            currentState.copy(daysOfWeek = list.sorted(), repeatIntervalDays = 1)
        }
    }

    fun onReminderTimeChange(time: String) {
        _state.update { it.copy(reminderTime = time) }
    }

    fun onRepeatHoursChange(hours: Int) {
        _state.update { it.copy(reminderRepeatHours = hours) }
    }

    fun onRemindUntilDoneChange(enabled: Boolean) {
        _state.update { it.copy(remindUntilDone = enabled) }
    }

    fun onCutoffTimeChange(time: String) {
        _state.update { it.copy(cutoffTime = time) }
    }

    fun onGracePeriodChange(enabled: Boolean) {
        _state.update { it.copy(gracePeriodUntil3AM = enabled) }
    }

    fun onVibrateChange(enabled: Boolean) {
        _state.update { it.copy(vibrate = enabled) }
    }

    fun onPersistentChange(enabled: Boolean) {
        _state.update { it.copy(persistent = enabled) }
    }

    fun saveStreak() {
        val currentState = _state.value
        if (currentState.title.trim().isEmpty()) {
            _state.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                val todayStr = AppClock.now().toString()
                val streak = Streak(
                    id = currentState.id,
                    title = currentState.title.trim(),
                    description = currentState.description.trim(),
                    emoji = currentState.emoji,
                    category = currentState.category,
                    color = currentState.color,
                    creationDate = if (currentState.id > 0 && currentState.creationDate.isNotEmpty()) {
                        currentState.creationDate
                    } else todayStr,
                    repeatIntervalDays = currentState.repeatIntervalDays,
                    daysOfWeek = currentState.daysOfWeek,
                    reminderTime = currentState.reminderTime,
                    reminderRepeatHours = currentState.reminderRepeatHours,
                    remindUntilDone = currentState.remindUntilDone,
                    dangerReminderMinutes = currentState.dangerReminderMinutes,
                    cutoffTime = currentState.cutoffTime,
                    gracePeriodUntil3AM = currentState.gracePeriodUntil3AM,
                    notificationSound = currentState.notificationSound,
                    vibrate = currentState.vibrate,
                    silent = currentState.silent,
                    persistent = currentState.persistent,
                    // If editing, keep these fields or update them
                    currentStreak = if (currentState.id > 0) {
                        streakUseCases.getStreakById(currentState.id).first()?.currentStreak ?: 0
                    } else 0,
                    longestStreak = if (currentState.id > 0) {
                        streakUseCases.getStreakById(currentState.id).first()?.longestStreak ?: 0
                    } else 0,
                    lastCompletedDate = if (currentState.id > 0) {
                        streakUseCases.getStreakById(currentState.id).first()?.lastCompletedDate ?: ""
                    } else ""
                )

                if (currentState.id > 0) {
                    streakUseCases.updateStreak(streak)
                } else {
                    streakUseCases.createStreak(streak)
                }
                _state.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.localizedMessage ?: "Unknown error") }
            }
        }
    }
}
