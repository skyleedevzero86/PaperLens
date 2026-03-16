package com.sleekydz86.paperlens.domain.port

import com.sleekydz86.paperlens.domain.user.User
import com.sleekydz86.paperlens.domain.user.UserRole

interface UserRepositoryPort {

    fun createUser(email: String, encodedPassword: String, name: String, role: UserRole): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}
