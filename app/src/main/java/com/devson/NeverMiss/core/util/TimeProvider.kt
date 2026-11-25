package com.devson.NeverMiss.core.util

import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun currentTimeMillis(): Long
}

@Singleton
class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
