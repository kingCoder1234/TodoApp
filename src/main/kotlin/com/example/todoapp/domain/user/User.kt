package com.example.todoapp.domain.user

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_users_email", columnNames = ["email"]),
        UniqueConstraint(name = "uq_users_username", columnNames = ["username"])
    ]
)
class User(
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var passwordHash: String?
)