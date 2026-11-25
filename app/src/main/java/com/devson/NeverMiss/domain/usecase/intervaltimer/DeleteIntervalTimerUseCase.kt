package com.devson.NeverMiss.domain.usecase.intervaltimer

import com.devson.NeverMiss.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class DeleteIntervalTimerUseCase @Inject constructor(
    private val repository: IntervalTimerRepository
) {
    suspend operator fun invoke(id: Long): Boolean {
        if (id <= 0L) return false
        return repository.deleteTimerById(id)
    }
}
