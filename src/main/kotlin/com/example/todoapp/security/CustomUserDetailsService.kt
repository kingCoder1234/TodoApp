package com.example.todoapp.security

import com.example.todoapp.domain.user.UserRepository
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): AuthUser {
        val user = userRepository.findByEmail(username.trim().lowercase())
            .orElseThrow { UsernameNotFoundException("User not found") }

        val id = user.id ?: throw IllegalStateException("User id missing")
        val hash = user.passwordHash ?: throw IllegalStateException("Password hash missing")

        return AuthUser(
            id = id,
            email = user.email,
            passwordHash = hash,
            roles = listOf("USER")
        )
    }
}