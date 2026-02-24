package com.example.todoapp.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig(
    private val appProperties: AppProperties
) {

    @Bean
    fun cacheManager(): CacheManager {
        val mgr = CaffeineCacheManager(
            "todoList", "todoSingle", "todoListVersion", "userProfile"
        )

        mgr.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(appProperties.cache.maxEntries)
                .expireAfterWrite(Duration.ofMinutes(appProperties.cache.todoListTtlMinutes))
        )

        return mgr
    }
}