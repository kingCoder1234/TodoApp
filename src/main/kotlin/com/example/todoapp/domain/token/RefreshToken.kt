package com.example.todoapp.domain.token

import com.example.todoapp.domain.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "ix_refresh_tokens_hash", columnList = "tokenHash", unique = true),
        Index(name = "ix_refresh_tokens_user", columnList = "user_id")
    ]
)
class RefreshToken(
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, length = 64, unique = true)
    var tokenHash: String,

    @Column(nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    var revokedAt: Instant? = null,

    var replacedByTokenId: UUID? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)