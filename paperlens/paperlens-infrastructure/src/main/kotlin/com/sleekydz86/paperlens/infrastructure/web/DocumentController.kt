package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.dto.DocumentUpdateRequest
import com.sleekydz86.paperlens.application.usecase.DocumentUseCase
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import com.sleekydz86.paperlens.infrastructure.persistence.entity.UserEntity
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentUseCase: DocumentUseCase,
    private val redisCacheService: RedisCacheService,
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart file: MultipartFile,
        @RequestPart title: String,
        @RequestPart(required = false) description: String?,
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Any> {
        val response = documentUseCase.uploadDocument(
            fileBytes = file.bytes,
            originalFileName = file.originalFilename ?: "document.pdf",
            title = title,
            description = description,
            userId = user.id,
        )
        redisCacheService.evictDocumentCaches()
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) docType: String?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(name = "tag", required = false) tag: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Any> {
        val normalizedTags = normalizeTagParams(tags, tag)
        redisCacheService.getDocumentList(keyword, docType, normalizedTags, page, size)
            ?.let { return ResponseEntity.ok(it) }

        val response = documentUseCase.getDocuments(keyword, docType, normalizedTags, page, size)
        redisCacheService.putDocumentList(keyword, docType, normalizedTags, page, size, response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/tags")
    fun tags(): ResponseEntity<Any> {
        redisCacheService.getAvailableTags()?.let { return ResponseEntity.ok(it) }

        val response = documentUseCase.getAvailableTags()
        redisCacheService.putAvailableTags(response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<Any> {
        redisCacheService.getDocumentDetail(id)?.let { return ResponseEntity.ok(it) }
        val response = documentUseCase.getDocument(id)
        redisCacheService.putDocumentDetail(id, response)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: DocumentUpdateRequest): ResponseEntity<Any> {
        val response = documentUseCase.updateDocument(id, request)
        redisCacheService.evictDocumentCaches()
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        documentUseCase.deleteDocument(id)
        redisCacheService.evictDocumentCaches()
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/download")
    fun download(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val (fileName, bytes) = documentUseCase.downloadFile(id)
        val disposition = ContentDisposition.attachment()
            .filename(fileName, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes)
    }
}
