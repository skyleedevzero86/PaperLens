package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.TokenPort
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class TokenAdapter(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration:86400000}") private val expiration: Long,
) : TokenPort {

    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    override fun generateToken(username: String, roles: List<String>): String =
        Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()

    fun extractUsername(token: String): String = parseClaims(token).subject

    fun isTokenValid(token: String): Boolean = try {
        parseClaims(token).expiration.after(Date())
    } catch (_: Exception) {
        false
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}