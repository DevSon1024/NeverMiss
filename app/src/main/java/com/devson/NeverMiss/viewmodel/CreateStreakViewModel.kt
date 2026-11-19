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

const val MAX_STREAK_TITLE_LENGTH = 50
const val MAX_STREAK_DESCRIPTION_LENGTH = 250

@Immutable
data class CreateStreakState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val icon: String = "auto_awesome",
    val emoji: String = "✨",
    val category: StreakCategory = StreakCategory.CUSTOM,
    val color: Int = 0xFF6200EE.toInt(),
    val creationDate: String = "",
    val daysOfWeek: List<Int> = emptyList(), // empty = Everyday, 1=Mon .. 7=Sun
    val reminderTime: String = "10:00", // HH:mm
    val initialStreak: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val titleError: String? = null,
    val descriptionError: String? = null,
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
                    it.copy(
                        id = streak.id,
                        title = streak.title,
                        description = streak.description,
                        icon = streak.icon ?: "auto_awesome",
                        emoji = streak.emoji,
                        category = streak.category,
                        color = streak.color,
                        creationDate = streak.creationDate,
                        daysOfWeek = streak.daysOfWeek,
                        reminderTime = streak.reminderTime,
                        initialStreak = if (streak.currentStreak > 0) streak.currentStreak.toString() else "",
                        titleError = null,
                        descriptionError = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        val trimmed = if (title.length > MAX_STREAK_TITLE_LENGTH) {
            title.take(MAX_STREAK_TITLE_LENGTH)
        } else {
            title
        }
        _state.update {
            it.copy(
                title = trimmed,
                titleError = null,
                errorMessage = null
            )
        }
    }

    fun onDescriptionChange(desc: String) {
        val trimmed = if (desc.length > MAX_STREAK_DESCRIPTION_LENGTH) {
            desc.take(MAX_STREAK_DESCRIPTION_LENGTH)
        } else {
            desc
        }
        _state.update {
            it.copy(
                description = trimmed,
                descriptionError = null
            )
        }
    }

    fun onIconChange(iconKey: String) {
        _state.update { it.copy(icon = iconKey) }
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

    fun onInitialStreakChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(5)
        _state.update { it.copy(initialStreak = digitsOnly) }
    }

    fun saveStreak() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isSaved) return

        val cleanTitle = currentState.title.trim()
        if (cleanTitle.isEmpty()) {
            _state.update {
                it.copy(
                    titleError = "Streak name is required",
                    errorMessage = "Please enter a title for your routine"
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val todayStr = AppClock.now().toString()

                if (currentState.id > 0) {
                    val existingStreak = streakUseCases.getStreakById(currentState.id).first()
                    if (existingStreak == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Streak not found"
                            )
                        }
                        return@launch
                    }

                    val updatedStreak = existingStreak.copy(
                        title = cleanTitle,
                        description = currentState.description.trim(),
                        icon = currentState.icon,
                        daysOfWeek = currentState.daysOfWeek,
                        reminderTime = currentState.reminderTime,
                        updatedAt = System.currentTimeMillis()
                    )

                    streakUseCases.updateStreak(
                        streak = updatedStreak,
                        updatedStreakCount = null
                    )
                } else {
                    val initialStreakCount = currentState.initialStreak.toIntOrNull() ?: 0
                    val streak = Streak(
                        title = cleanTitle,
                        description = currentState.description.trim(),
                        icon = currentState.icon,
                        emoji = currentState.emoji,
                        category = currentState.category,
                        color = currentState.color,
                        creationDate = todayStr,
                        startDate = todayStr,
                        repeatIntervalDays = 1,
                        daysOfWeek = currentState.daysOfWeek,
                        reminderTime = currentState.reminderTime,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastCompletedDate = "",
                        updatedAt = System.currentTimeMillis()
                    )
                    streakUseCases.createStreak(streak, initialStreakCount)
                }

                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to save streak"
                    )
                }
            }
        }
    }
}
