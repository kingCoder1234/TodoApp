package com.example.todoapp.ratelimit

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class RateLimiterStore {

    private data class BucketState(
        var windowStart: Long,
        var remaining: Long
    )

    data class Result(
        val allowed: Boolean,
        val limit: Long,
        val remaining: Long,
        val resetEpochSeconds: Long,
        val retryAfterSeconds: Long
    )

    private val buckets = ConcurrentHashMap<String, BucketState>()

    fun consume(key: String, limit: Long, windowSeconds: Long): Result {
        val now = Instant.now().epochSecond
        val windowStart = now - (now % windowSeconds)
        val reset = windowStart + windowSeconds

        val state = buckets.compute(key) { _, existing ->
            if (existing == null || existing.windowStart != windowStart) {
                BucketState(windowStart = windowStart, remaining = limit)
            } else {
                existing
            }
        }!!

        return synchronized(state) {
            if (state.windowStart != windowStart) {
                state.windowStart = windowStart
                state.remaining = limit
            }

            if (state.remaining > 0) {
                state.remaining -= 1
                Result(
                    allowed = true,
                    limit = limit,
                    remaining = state.remaining,
                    resetEpochSeconds = reset,
                    retryAfterSeconds = 0
                )
            } else {
                val retryAfter = (reset - now).coerceAtLeast(0)
                Result(
                    allowed = false,
                    limit = limit,
                    remaining = 0,
                    resetEpochSeconds = reset,
                    retryAfterSeconds = retryAfter
                )
            }
        }
    }
}