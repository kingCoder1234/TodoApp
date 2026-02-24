package com.example.todoapp.security

import org.springframework.security.core.AuthenticationException

class JwtAuthenticationFailedException(message: String) : AuthenticationException(message)