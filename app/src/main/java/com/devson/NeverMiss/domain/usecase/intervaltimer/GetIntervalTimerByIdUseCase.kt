package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIntervalTimerByIdUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    suspend operator fun invoke(id: Long): IntervalTimer? {
        if (id <= 0L) return null
        return repository.getTimerById(id)
    }

    fun observe(id: Long): Flow<IntervalTimer?> {
        return repository.observeTimerById(id)
    }
}
