package com.example.todoapp.exception

import java.time.Instant

data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: String = Instant.now().toString()
)