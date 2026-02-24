package com.example.todoapp.api.auth

import com.example.todoapp.api.auth.dto.AuthResponse
import com.example.todoapp.api.auth.dto.LoginRequest
import com.example.todoapp.api.auth.dto.RefreshRequest
import com.example.todoapp.api.auth.dto.RegisterRequest
import com.example.todoapp.logging.logger
import com.example.todoapp.service.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    private val logger = logger()

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ResponseEntity<AuthResponse> {
        logger.info("Auth register attempt emailDomain={}", req.email.substringAfter("@", "unknown"))
        val result = authService.register(req)
        logger.info("Auth register success emailDomain={}", req.email.substringAfter("@", "unknown"))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<AuthResponse> {
        logger.info("Auth login attempt emailDomain={}", req.email.substringAfter("@", "unknown"))
        val result = authService.login(req)
        logger.info("Auth login success emailDomain={}", req.email.substringAfter("@", "unknown"))
        return ResponseEntity.ok(result)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshRequest): ResponseEntity<AuthResponse> {
        // Do NOT log refresh token
        logger.info("Auth refresh attempt")
        val result = authService.refresh(req.refreshToken)
        logger.info("Auth refresh success")
        return ResponseEntity.ok(result)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody req: RefreshRequest) {
        // Do NOT log refresh token
        logger.info("Auth logout attempt")
        authService.logout(req.refreshToken)
        logger.info("Auth logout success")
    }
}