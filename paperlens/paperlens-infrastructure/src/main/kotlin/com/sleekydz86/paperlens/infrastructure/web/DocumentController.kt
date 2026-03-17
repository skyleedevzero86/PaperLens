package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.dto.DocumentUpdateRequest
import com.sleekydz86.paperlens.application.usecase.DocumentUseCase
import com.sleekydz86.paperlens.infrastructure.persistence.entity.UserEntity
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.nio.charset.StandardCharsets
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
class DocumentController(private val documentUseCase: DocumentUseCase) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart file: MultipartFile,
        @RequestPart title: String,
        @RequestPart(required = false) description: String?,
        @AuthenticationPrincipal user: UserEntity,
    ) = ResponseEntity.ok(
        documentUseCase.uploadDocument(
            fileBytes = file.bytes,
            originalFileName = file.originalFilename ?: "document.pdf",
            title = title,
            description = description,
            userId = user.id,
        )
    )

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) docType: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ResponseEntity.ok(documentUseCase.getDocuments(keyword, docType, page, size))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) =
        ResponseEntity.ok(documentUseCase.getDocument(id))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: DocumentUpdateRequest) =
        ResponseEntity.ok(documentUseCase.updateDocument(id, request))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        documentUseCase.deleteDocument(id)
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