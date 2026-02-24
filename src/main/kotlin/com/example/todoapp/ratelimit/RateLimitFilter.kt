package com.example.todoapp.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

class RateLimitFilter(
    private val store: RateLimiterStore
) : OncePerRequestFilter() {

    private val authUserLimit = 200L
    private val authUserWindow = Duration.ofMinutes(1).seconds

    private val anonLimit = 30L
    private val anonWindow = Duration.ofMinutes(1).seconds

    private val authEndpointsLimit = 10L
    private val authEndpointsWindow = Duration.ofMinutes(15).seconds

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: "/"
        val isAuthEndpoint = path.startsWith("/api/auth")

        val auth = SecurityContextHolder.getContext().authentication
        val isAuthenticated =
            auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken

        val (limit, windowSeconds, identityKey) = when {
            isAuthEndpoint -> {
                val ip = ClientIpResolver.resolve(request)
                Triple(authEndpointsLimit, authEndpointsWindow, "auth-ip:$ip")
            }
            isAuthenticated -> {
                // Prefer a stable identifier (userId) if your JwtAuthenticationFilter sets it as auth.name.
                val userKey = auth.name ?: "unknown"
                Triple(authUserLimit, authUserWindow, "user:$userKey")
            }
            else -> {
                val ip = ClientIpResolver.resolve(request)
                Triple(anonLimit, anonWindow, "anon-ip:$ip")
            }
        }

        val result = store.consume(identityKey, limit, windowSeconds)

        response.setHeader("X-RateLimit-Limit", result.limit.toString())
        response.setHeader("X-RateLimit-Remaining", result.remaining.toString())
        response.setHeader("X-RateLimit-Reset", result.resetEpochSeconds.toString())

        if (!result.allowed) {
            response.status = 429
            response.setHeader("Retry-After", result.retryAfterSeconds.toString())
            response.contentType = "application/json"
            response.writer.write(
                """{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded","path":"$path"}"""
            )
            return
        }

        filterChain.doFilter(request, response)
    }
}