package com.devson.NeverMiss.domain.usecase

import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveStreaksUseCase @Inject constructor(
    private val repository: StreakRepository
) {
    operator fun invoke(): Flow<List<Streak>> {
        return repository.getActiveStreaksFlow()
    }
}
