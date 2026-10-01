package com.devson.NeverMiss.viewmodel

import androidx.compose.runtime.Immutable
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

@Immutable
data class CreateStreakState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val emoji: String = "✨",
    val category: StreakCategory = StreakCategory.CUSTOM,
    val color: Int = 0xFF6200EE.toInt(),
    val creationDate: String = "",
    val daysOfWeek: List<Int> = emptyList(), // empty = Everyday, 1=Mon .. 7=Sun
    val reminderTime: String = "10:00", // HH:mm
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
                        daysOfWeek = streak.daysOfWeek,
                        reminderTime = streak.reminderTime
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

    fun setEveryday() {
        _state.update { it.copy(daysOfWeek = emptyList()) }
    }

    fun toggleDayOfWeek(day: Int) {
        _state.update { currentState ->
            val list = currentState.daysOfWeek.toMutableList()
            if (list.contains(day)) {
                list.remove(day)
            } else {
                list.add(day)
            }
            currentState.copy(daysOfWeek = list.sorted())
        }
    }

    fun onReminderTimeChange(time: String) {
        _state.update { it.copy(reminderTime = time) }
    }

    fun saveStreak() {
        val currentState = _state.value
        if (currentState.title.trim().isEmpty()) {
            _state.update { it.copy(errorMessage = "Please enter a title for your routine") }
            return
        }

        viewModelScope.launch {
            try {
                val todayStr = AppClock.now().toString()
                val existingStreak = if (currentState.id > 0) {
                    streakUseCases.getStreakById(currentState.id).first()
                } else null

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
                    repeatIntervalDays = 1,
                    daysOfWeek = currentState.daysOfWeek,
                    reminderTime = currentState.reminderTime,
                    currentStreak = existingStreak?.currentStreak ?: 0,
                    longestStreak = existingStreak?.longestStreak ?: 0,
                    lastCompletedDate = existingStreak?.lastCompletedDate ?: ""
                )

                if (currentState.id > 0) {
                    streakUseCases.updateStreak(streak)
                } else {
                    streakUseCases.createStreak(streak)
                }
                _state.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.localizedMessage ?: "Failed to save streak") }
            }
        }
    }
}
