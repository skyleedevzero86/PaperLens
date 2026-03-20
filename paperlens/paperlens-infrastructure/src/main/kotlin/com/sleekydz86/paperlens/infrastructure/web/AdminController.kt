package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.usecase.AdminUseCase
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import com.sleekydz86.paperlens.infrastructure.global.session.RedisSessionStoreService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminUseCase: AdminUseCase,
    private val redisCacheService: RedisCacheService,
    private val sessionStoreService: RedisSessionStoreService,
) {

    @GetMapping("/stats")
    fun stats(): ResponseEntity<Any> {
        redisCacheService.getAdminStats()?.let { return ResponseEntity.ok(it) }
        val response = adminUseCase.getStats()
        redisCacheService.putAdminStats(response)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/documents/{id}/reprocess")
    fun reprocess(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        adminUseCase.reprocess(id)
        redisCacheService.evictDocumentCaches()
        return ResponseEntity.ok(mapOf("message" to "??荑귞뵳?? ??뽰삂??뤿???щ빍??"))
    }

    @GetMapping("/failed-documents")
    fun failedDocuments(): ResponseEntity<Any> {
        redisCacheService.getFailedDocuments()?.let { return ResponseEntity.ok(it) }
        val response = adminUseCase.getFailedDocuments()
        redisCacheService.putFailedDocuments(response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/document-jobs")
    fun documentJobs(
        @RequestParam(defaultValue = "50") limit: Int,
    ): ResponseEntity<Any> {
        redisCacheService.getRecentDocumentJobs(limit)?.let { return ResponseEntity.ok(it) }
        val response = adminUseCase.getRecentDocumentJobs(limit)
        redisCacheService.putRecentDocumentJobs(limit, response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/documents/{id}/jobs")
    fun documentJobsByDocument(@PathVariable id: Long): ResponseEntity<Any> {
        redisCacheService.getDocumentJobs(id)?.let { return ResponseEntity.ok(it) }
        val response = adminUseCase.getDocumentJobs(id)
        redisCacheService.putDocumentJobs(id, response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cache-stats")
    fun cacheStats() = ResponseEntity.ok(redisCacheService.getStats())

    @GetMapping("/sessions")
    fun sessions(@RequestParam(defaultValue = "100") limit: Int) =
        ResponseEntity.ok(sessionStoreService.getActiveSessions(limit))
}
