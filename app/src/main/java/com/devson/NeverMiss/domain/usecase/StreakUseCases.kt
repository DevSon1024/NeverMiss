package com.devson.NeverMiss.domain.usecase

import javax.inject.Inject

data class StreakUseCases @Inject constructor(
    val getActiveStreaks: GetActiveStreaksUseCase,
    val getAllStreaks: GetAllStreaksUseCase,
    val getStreakById: GetStreakByIdUseCase,
    val createStreak: CreateStreakUseCase,
    val updateStreak: UpdateStreakUseCase,
    val deleteStreak: DeleteStreakUseCase,
    val logCompletion: LogCompletionUseCase,
    val checkAndResetMissedStreaks: CheckAndResetMissedStreaksUseCase
)
