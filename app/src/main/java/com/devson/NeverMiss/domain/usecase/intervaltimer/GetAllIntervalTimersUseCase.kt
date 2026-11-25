package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllIntervalTimersUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    operator fun invoke(): Flow<List<IntervalTimer>> {
        return repository.observeAllTimers()
    }
}
