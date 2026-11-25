package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class GetIntervalTimersEligibleForRecoveryUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    suspend operator fun invoke(): List<IntervalTimer> {
        return repository.getTimersEligibleForRecovery()
    }
}
