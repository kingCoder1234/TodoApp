package com.example.todoapp.service.auth

import com.example.todoapp.api.auth.dto.AuthResponse
import com.example.todoapp.api.auth.dto.LoginRequest
import com.example.todoapp.api.auth.dto.RegisterRequest
import com.example.todoapp.config.JwtProperties
import com.example.todoapp.domain.user.User
import com.example.todoapp.domain.user.UserRepository
import com.example.todoapp.exception.ConflictException
import com.example.todoapp.exception.UnauthorizedException
import com.example.todoapp.logging.logger
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val refreshTokenService: RefreshTokenService,
    private val jwtProperties: JwtProperties
) {
    private val logger = logger()

    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val username = req.username.trim()

        logger.info("Register attempt emailDomain={} username={}", email.substringAfter("@", "unknown"), username)

        if (userRepository.existsByEmail(email)) {
            logger.warn("Register rejected: email exists emailDomain={}", email.substringAfter("@", "unknown"))
            throw ConflictException("Email already exists")
        }
        if (userRepository.existsByUsername(username)) {
            logger.warn("Register rejected: username exists username={}", username)
            throw ConflictException("Username already exists")
        }

        val user = User(
            email = email,
            username = username,
            passwordHash = passwordEncoder.encode(req.password)
        )

        val saved = userRepository.save(user)

        val access = tokenService.createAccessToken(saved)
        val refreshValue = tokenService.createRefreshTokenValue()
        refreshTokenService.issue(saved, refreshValue)

        logger.info("Register success userId={}", saved.id)

        return AuthResponse(
            accessToken = access,
            refreshToken = refreshValue,
            accessTokenExpiresInSeconds = jwtProperties.accessTokenExpirationSeconds
        )
    }

    @Transactional
    fun login(req: LoginRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        logger.info("Login attempt emailDomain={}", email.substringAfter("@", "unknown"))

        val user = userRepository.findByEmail(email)
            .orElseThrow {
                logger.warn("Login failed: user not found emailDomain={}", email.substringAfter("@", "unknown"))
                UnauthorizedException("Invalid credentials")
            }

        if (!passwordEncoder.matches(req.password, user.passwordHash)) {
            logger.warn("Login failed: bad password userId={}", user.id)
            throw UnauthorizedException("Invalid credentials")
        }

        val userId = user.id ?: throw IllegalStateException("User id missing")

        // common: revoke old refresh tokens on login
        refreshTokenService.revokeAllForUser(userId)

        val access = tokenService.createAccessToken(user)
        val refreshValue = tokenService.createRefreshTokenValue()
        refreshTokenService.issue(user, refreshValue)

        logger.info("Login success userId={}", userId)

        return AuthResponse(
            accessToken = access,
            refreshToken = refreshValue,
            accessTokenExpiresInSeconds = jwtProperties.accessTokenExpirationSeconds
        )
    }

    @Transactional
    fun refresh(refreshToken: String): AuthResponse {
        // Do NOT log token
        logger.info("Refresh attempt")

        val raw = refreshToken.trim()
        if (raw.isBlank()) {
            logger.warn("Refresh failed: blank token")
            throw UnauthorizedException("Invalid refresh token")
        }

        val rotated = refreshTokenService.rotate(raw)
        val access = tokenService.createAccessToken(rotated.user)

        logger.info("Refresh success userId={}", rotated.user.id)

        return AuthResponse(
            accessToken = access,
            refreshToken = rotated.newRefreshTokenValue,
            accessTokenExpiresInSeconds = jwtProperties.accessTokenExpirationSeconds
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        // Do NOT log token
        logger.info("Logout attempt")

        val raw = refreshToken.trim()
        if (raw.isBlank()) {
            logger.warn("Logout failed: blank token")
            throw UnauthorizedException("Invalid refresh token")
        }

        refreshTokenService.revoke(raw)
        logger.info("Logout success")
    }
}