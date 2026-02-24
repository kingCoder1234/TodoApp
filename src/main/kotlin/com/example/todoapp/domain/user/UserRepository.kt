
package com.example.todoapp.domain.user;

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun findByEmail(email: String): java.util.Optional<User>
    fun findByIdAndEmail(id: UUID, email: String): User?
}