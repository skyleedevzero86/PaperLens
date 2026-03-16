package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.domain.port.UserRepositoryPort
import com.sleekydz86.paperlens.domain.user.User
import com.sleekydz86.paperlens.domain.user.UserRole
import com.sleekydz86.paperlens.infrastructure.persistence.entity.UserEntity
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.UserMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component

@Component
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
) : UserRepositoryPort {

    override fun createUser(email: String, encodedPassword: String, name: String, role: UserRole): User {
        val entity = UserEntity(
            email = email,
            password = encodedPassword,
            name = name,
            role = role,
        )
        val saved = jpaRepository.save(entity)
        return UserMapper.toDomain(saved)
    }

    override fun findById(id: Long): User? =
        jpaRepository.findById(id).map { UserMapper.toDomain(it) }.orElse(null)

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.let { UserMapper.toDomain(it) }

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email)
}