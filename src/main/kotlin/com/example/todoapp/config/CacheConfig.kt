package com.example.todoapp.config

import java.time.Duration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
@EnableCaching
class CacheConfig(
        private val appProperties: AppProperties,
        private val redisConnectionFactory: RedisConnectionFactory
) {

    @Bean
    fun cacheManager(): CacheManager {
        val base = RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()

        val todoListCfg = base.entryTtl(Duration.ofMinutes(appProperties.cache.todoListTtlMinutes))
        val todoSingleCfg =
                base.entryTtl(Duration.ofMinutes(appProperties.cache.todoSingleTtlMinutes))
        val userProfileCfg =
                base.entryTtl(Duration.ofMinutes(appProperties.cache.profileTtlMinutes))

        // NOTE:
        // - max-entries is a Caffeine concept. Redis doesn't enforce local maximumSize.
        // - version-ttl-minutes + todoListVersion cache are not used in the Redis-annotation
        // approach.
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration("todoList", todoListCfg)
                .withCacheConfiguration("todoSingle", todoSingleCfg)
                .withCacheConfiguration("userProfile", userProfileCfg)
                .build()
    }
}