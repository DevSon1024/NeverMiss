package com.devson.NeverMiss.domain.usecase.intervaltimer

import javax.inject.Inject

data class IntervalTimerUseCases @Inject constructor(
    val createTimer: CreateIntervalTimerUseCase,
    val updateTimer: UpdateIntervalTimerUseCase,
    val deleteTimer: DeleteIntervalTimerUseCase,
    val getTimerById: GetIntervalTimerByIdUseCase,
    val getAllTimers: GetAllIntervalTimersUseCase,
    val getTimersByStatus: GetIntervalTimersByStatusUseCase,
    val getTimersEligibleForRecovery: GetIntervalTimersEligibleForRecoveryUseCase
)
