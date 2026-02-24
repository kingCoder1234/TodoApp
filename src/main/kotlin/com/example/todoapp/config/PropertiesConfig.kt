package com.example.todoapp.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AppProperties::class,
    JwtProperties::class,
    AuthProperties::class
)
class PropertiesConfig