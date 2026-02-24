package com.example.todoapp

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.redis.core.StringRedisTemplate

@SpringBootApplication
class TodoappApplication {

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @PostConstruct
    fun testRedis() {
        redisTemplate.opsForValue().set("test", "working")
        println(redisTemplate.opsForValue().get("test"))
    }
}

fun main(args: Array<String>) {
    runApplication<TodoappApplication>(*args)
}