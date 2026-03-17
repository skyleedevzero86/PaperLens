package com.sleekydz86.paperlens.infrastructure.security

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordHashVerificationTest {

    private val encoder = BCryptPasswordEncoder()

    @Test
    fun printHashedPassword() {
        val rawPassword = "root123"
        val hash = encoder.encode(rawPassword)
        println("raw=$rawPassword")
        println("hash=$hash")
        assertTrue(hash.isNotBlank())
    }
}

