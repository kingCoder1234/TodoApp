package com.example.todoapp.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class AuthUser(
    val id: UUID,
    private val email: String,
    private val passwordHash: String,
    private val roles: List<String> = listOf("USER")
) : UserDetails {

    override fun getUsername(): String = email
    override fun getPassword(): String = passwordHash

    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_$it") }

    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}