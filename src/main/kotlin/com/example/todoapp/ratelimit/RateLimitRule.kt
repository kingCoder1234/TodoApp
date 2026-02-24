package com.example.todoapp.ratelimit

import java.time.Duration

data class RateLimitRule(
    val name: String,
    val capacity: Long,
    val window: Duration
)