package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.AuthResponse
import com.sleekydz86.paperlens.application.dto.LoginRequest
import com.sleekydz86.paperlens.application.dto.RegisterRequest
import com.sleekydz86.paperlens.application.exception.DuplicateEmailException
import com.sleekydz86.paperlens.application.exception.InvalidCredentialsException
import com.sleekydz86.paperlens.application.port.AuthPort
import com.sleekydz86.paperlens.application.port.PasswordEncoderPort
import com.sleekydz86.paperlens.application.port.TokenPort
import com.sleekydz86.paperlens.domain.port.UserRepositoryPort
import com.sleekydz86.paperlens.domain.user.UserRole

class AuthUseCase(
    private val userRepository: UserRepositoryPort,
    private val passwordEncoder: PasswordEncoderPort,
    private val tokenPort: TokenPort,
    private val authPort: AuthPort,
) {

    fun login(request: LoginRequest): AuthResponse {
        val user = authPort.authenticate(request.email, request.password)
            ?: throw InvalidCredentialsException()
        val token = tokenPort.generateToken(user.email, listOf("ROLE_${user.role.name}"))
        return AuthResponse(token, user.email, user.name, user.role.name)
    }

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException()
        }
        val user = userRepository.createUser(
            email = request.email,
            encodedPassword = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.USER,
        )
        val token = tokenPort.generateToken(user.email, listOf("ROLE_USER"))
        return AuthResponse(token, user.email, user.name, user.role.name)
    }
}
