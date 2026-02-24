package com.example.todoapp.ratelimit

import jakarta.servlet.http.HttpServletRequest

object ClientIpResolver {
    fun resolve(request: HttpServletRequest): String {
        val xff = request.getHeader("X-Forwarded-For")
        if (!xff.isNullOrBlank()) return xff.split(",").first().trim()
        return request.remoteAddr ?: "unknown"
    }
}