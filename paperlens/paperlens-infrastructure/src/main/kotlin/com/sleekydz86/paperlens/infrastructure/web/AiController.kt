package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.dto.QaRequest
import com.sleekydz86.paperlens.application.usecase.AiUseCase
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import com.sleekydz86.paperlens.infrastructure.persistence.entity.UserEntity
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiUseCase: AiUseCase,
    private val redisCacheService: RedisCacheService,
    @org.springframework.beans.factory.annotation.Value("\${app.ai.chat-model-name:huggingface}") private val chatModelName: String,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/qa")
    fun ask(
        @RequestBody request: QaRequest,
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<*> {
        val start = System.currentTimeMillis()
        val response = aiUseCase.answerQuestion(request)
        val latency = System.currentTimeMillis() - start
        runCatching {
            aiUseCase.logQuery(user.id, request.documentId, request.question, response.answer, latency, chatModelName)
        }.onFailure { ex ->
            logger.warn("AI 吏덉쓽 濡쒓렇 ????ㅽ뙣: user={}, document={}", user.id, request.documentId, ex)
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/similar/{documentId}")
    fun similar(@PathVariable documentId: Long): ResponseEntity<Any> {
        val limit = 5
        redisCacheService.getSimilarDocuments(documentId, limit)?.let { return ResponseEntity.ok(it) }
        val response = aiUseCase.findSimilarDocuments(documentId, limit)
        redisCacheService.putSimilarDocuments(documentId, limit, response)
        return ResponseEntity.ok(response)
    }
}
