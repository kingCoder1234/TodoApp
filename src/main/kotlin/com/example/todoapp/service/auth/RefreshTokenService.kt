package com.example.todoapp.service.auth

import com.example.todoapp.config.AuthProperties
import com.example.todoapp.domain.token.RefreshToken
import com.example.todoapp.domain.token.RefreshTokenRepository
import com.example.todoapp.domain.user.User
import com.example.todoapp.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class RefreshTokenService(
    private val repo: RefreshTokenRepository,
    private val tokenService: TokenService,
    private val authProperties: AuthProperties
) {
    private val refreshDays: Long get() = authProperties.refreshTokenDays

    fun issue(user: User, rawRefreshToken: String): RefreshToken {
        val now = Instant.now()
        val entity = RefreshToken(
            user = user,
            tokenHash = sha256Hex(rawRefreshToken),
            expiresAt = now.plus(refreshDays, ChronoUnit.DAYS),
            revoked = false,
            createdAt = now
        )
        return repo.save(entity)
    }

    @Transactional
    fun revokeAllForUser(userId: UUID) {
        repo.deleteByUserId(userId)
    }

    data class RotateResult(
        val user: User,
        val newRefreshTokenValue: String
    )

    @Transactional
    fun rotate(rawRefreshToken: String): RotateResult {
        val now = Instant.now()
        val hash = sha256Hex(rawRefreshToken)

        val existing = repo.findByTokenHash(hash)
            ?: throw UnauthorizedException("Invalid refresh token")

        if (existing.revoked) throw UnauthorizedException("Refresh token revoked")
        if (existing.expiresAt.isBefore(now)) throw UnauthorizedException("Refresh token expired")

        existing.revoked = true
        existing.revokedAt = now

        val newValue = tokenService.createRefreshTokenValue()
        val newEntity = RefreshToken(
            user = existing.user,
            tokenHash = sha256Hex(newValue),
            expiresAt = now.plus(refreshDays, ChronoUnit.DAYS),
            revoked = false,
            createdAt = now
        )
        val savedNew = repo.save(newEntity)

        existing.replacedByTokenId = savedNew.id
        repo.save(existing)

        return RotateResult(existing.user, newValue)
    }

    @Transactional
    fun revoke(rawRefreshToken: String) {
        val now = Instant.now()
        val hash = sha256Hex(rawRefreshToken)

        val token = repo.findByTokenHash(hash) ?: return

        if (!token.revoked) {
            token.revoked = true
            token.revokedAt = now
            repo.save(token)
        }
    }

    private fun sha256Hex(value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}