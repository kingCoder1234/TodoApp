package com.example.todoapp.api.profile.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProfileUpdateRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    val username: String
)