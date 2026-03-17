package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.usecase.AdminUseCase
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(private val adminUseCase: AdminUseCase) {

    @GetMapping("/stats")
    fun stats() = ResponseEntity.ok(adminUseCase.getStats())

    @PostMapping("/documents/{id}/reprocess")
    fun reprocess(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        adminUseCase.reprocess(id)
        return ResponseEntity.ok(mapOf("message" to "재처리가 시작되었습니다."))
    }

    @GetMapping("/failed-documents")
    fun failedDocuments() = ResponseEntity.ok(adminUseCase.getFailedDocuments())
}