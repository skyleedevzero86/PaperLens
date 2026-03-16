package com.sleekydz86.paperlens.domain.user

import java.time.LocalDateTime

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
) {
    val isActive: Boolean get() = deletedAt == null
}
