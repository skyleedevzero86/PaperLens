package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.dto.LoginRequest
import com.sleekydz86.paperlens.application.dto.RegisterRequest
import com.sleekydz86.paperlens.application.usecase.AuthUseCase
import com.sleekydz86.paperlens.infrastructure.global.session.RedisSessionStoreService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authUseCase: AuthUseCase,
    private val sessionStoreService: RedisSessionStoreService,
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val response = authUseCase.login(request)
        sessionStoreService.trackAuthenticatedSession(httpRequest, response)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val response = authUseCase.register(request)
        sessionStoreService.trackAuthenticatedSession(httpRequest, response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/session")
    fun currentSession(request: HttpServletRequest): ResponseEntity<Any> {
        val session = sessionStoreService.getCurrentSession(request) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(session)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        sessionStoreService.invalidateCurrentSession(request)
        return ResponseEntity.ok(mapOf("message" to "Logged out"))
    }
}
