package com.sleekydz86.paperlens.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class RegisterRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String,
    @field:NotBlank val name: String,
)

data class AuthResponse(
    val token: String,
    val email: String,
    val name: String,
    val role: String
)
