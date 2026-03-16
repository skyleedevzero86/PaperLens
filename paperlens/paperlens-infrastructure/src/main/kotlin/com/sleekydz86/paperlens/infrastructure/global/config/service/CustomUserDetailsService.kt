package com.sleekydz86.paperlens.infrastructure.global.config.service

import com.sleekydz86.paperlens.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component


@Component
class CustomUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String) =
        userJpaRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")
}
