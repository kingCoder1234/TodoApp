package com.example.todoapp.security

import com.example.todoapp.config.JwtProperties
import com.example.todoapp.domain.user.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val userDetailsService: UserDetailsService
) {
    companion object {
        const val CLAIM_UID = "uid"
        const val CLAIM_USERNAME = "uname"
    }

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateAccessToken(user: User): String {
        val userId = user.id ?: throw IllegalStateException("User id is required to generate token")
        val now = Instant.now()
        val exp = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds)

        return Jwts.builder()
            .issuer(jwtProperties.issuer)
            .subject(user.email)
            .claim(CLAIM_UID, userId.toString()) // store UUID as String
            .claim(CLAIM_USERNAME, user.username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact()
    }

    fun generateRefreshTokenValue(): String = UUID.randomUUID().toString()

    fun validateToken(token: String): Boolean =
        runCatching {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        }.isSuccess

    fun getClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    fun getEmail(token: String): String = getClaims(token).subject

    fun getUserId(token: String): UUID {
        val v = getClaims(token)[CLAIM_UID] ?: throw IllegalArgumentException("Missing claim: $CLAIM_UID")
        return UUID.fromString(v.toString())
    }

    fun getAuthentication(token: String): Authentication {
        val email = getEmail(token)
        val userDetails = userDetailsService.loadUserByUsername(email)
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }
}