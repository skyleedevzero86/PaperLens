package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.AuthPort
import com.sleekydz86.paperlens.domain.user.User
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.UserMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AuthAdapter(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
) : AuthPort {

    override fun authenticate(email: String, password: String): User? {
        val user = userJpaRepository.findByEmail(email) ?: return null
        if (!passwordEncoder.matches(password, user.getPassword())) return null
        return UserMapper.toDomain(user)
    }
}