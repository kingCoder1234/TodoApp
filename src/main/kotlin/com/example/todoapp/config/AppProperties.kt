package com.example.todoapp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val pagination: Pagination = Pagination(),
    val cache: Cache = Cache()
) {
    data class Pagination(
        val defaultSize: Int = 20,
        val maxSize: Int = 100
    )

    data class Cache(
        val todoListTtlMinutes: Long = 5,
        val todoSingleTtlMinutes: Long = 30,
        val profileTtlMinutes: Long = 30,
        val versionTtlMinutes: Long = 60,
        val maxEntries: Long = 10_000
    )
}