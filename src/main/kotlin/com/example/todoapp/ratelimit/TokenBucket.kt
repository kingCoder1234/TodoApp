package com.example.todoapp.ratelimit

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

/**
 * Fixed-window token bucket:
 * - capacity tokens per window
 * - at window boundary, tokens reset to capacity
 */
class TokenBucket(
    private val capacity: Long,
    private val windowSeconds: Long,
    nowSeconds: Long
) {
    private val lock = ReentrantLock()
    private var windowStartSeconds: Long = floorWindowStart(nowSeconds)
    private var tokensRemaining: Long = capacity

    private fun floorWindowStart(epochSeconds: Long): Long {
        return epochSeconds - (epochSeconds % windowSeconds)
    }

    data class Result(
        val allowed: Boolean,
        val limit: Long,
        val remaining: Long,
        val resetEpochSeconds: Long,
        val retryAfterSeconds: Long
    )

    fun tryConsume(tokens: Long = 1): Result {
        val nowSeconds = Instant.now().epochSecond
        lock.withLock {
            val currentWindowStart = floorWindowStart(nowSeconds)
            if (currentWindowStart != windowStartSeconds) {
                windowStartSeconds = currentWindowStart
                tokensRemaining = capacity
            }

            val reset = windowStartSeconds + windowSeconds
            val canConsume = tokensRemaining >= tokens

            return if (canConsume) {
                tokensRemaining -= tokens
                Result(
                    allowed = true,
                    limit = capacity,
                    remaining = tokensRemaining,
                    resetEpochSeconds = reset,
                    retryAfterSeconds = 0
                )
            } else {
                val retryAfter = (reset - nowSeconds).coerceAtLeast(0)
                Result(
                    allowed = false,
                    limit = capacity,
                    remaining = tokensRemaining,
                    resetEpochSeconds = reset,
                    retryAfterSeconds = retryAfter
                )
            }
        }
    }
}