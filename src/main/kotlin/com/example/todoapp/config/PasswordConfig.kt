package com.example.todoapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        // BCrypt default strength is 10; 12 is a common production choice.
        // If you see login/register latency issues, drop back to 10.
        return BCryptPasswordEncoder(12)
    }
}