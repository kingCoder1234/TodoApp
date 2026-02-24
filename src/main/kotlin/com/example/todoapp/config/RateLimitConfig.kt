package com.example.todoapp.config

import com.example.todoapp.ratelimit.RateLimitFilter
import com.example.todoapp.ratelimit.RateLimiterStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@Configuration
class RateLimitConfig {

    @Bean
    fun rateLimiterStore(): RateLimiterStore = RateLimiterStore()

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    fun rateLimitFilter(store: RateLimiterStore): RateLimitFilter {
        return RateLimitFilter(store)
    }
}