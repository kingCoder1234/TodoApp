package com.example.todoapp.config

import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheErrorHandlingConfig : CachingConfigurer {

    @Bean
    override fun errorHandler(): CacheErrorHandler {
        // Graceful fallback: if Redis is down, ignore cache failures and continue with DB.
        return object : CacheErrorHandler {
            override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {}
            override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {}
            override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {}
            override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {}
        }
    }
}