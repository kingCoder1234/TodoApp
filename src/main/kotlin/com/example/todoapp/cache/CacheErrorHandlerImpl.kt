package com.example.todoapp.cache

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

class CacheErrorHandlerImpl : CacheErrorHandler {

    private val log = LoggerFactory.getLogger(CacheErrorHandlerImpl::class.java)

    override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
        log.warn("Cache GET error on cache='{}' key='{}': {}", cache.name, key, exception.message)
    }

    override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
        log.warn("Cache PUT error on cache='{}' key='{}': {}", cache.name, key, exception.message)
    }

    override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
        log.warn("Cache EVICT error on cache='{}' key='{}': {}", cache.name, key, exception.message)
    }

    override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
        log.warn("Cache CLEAR error on cache='{}': {}", cache.name, exception.message)
    }
}