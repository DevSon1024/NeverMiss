package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIntervalTimersByStatusUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    operator fun invoke(status: IntervalTimerStatus): Flow<List<IntervalTimer>> {
        return repository.observeTimersByStatus(status)
    }
}
