package com.example.todoapp.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    private val email: String,
    private val passwordHash: String? = null,
    private val authoritiesList: List<GrantedAuthority> = emptyList()
) : UserDetails {

    override fun getAuthorities(): List<GrantedAuthority> = authoritiesList
    override fun getPassword(): String = passwordHash ?: ""
    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}