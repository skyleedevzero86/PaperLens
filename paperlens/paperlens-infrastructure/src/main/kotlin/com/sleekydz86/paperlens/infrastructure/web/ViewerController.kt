package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import java.nio.charset.StandardCharsets
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/viewer")
class ViewerController(
    private val documentRepository: DocumentRepositoryPort,
    private val fileStorage: FileStoragePort,
) {

    @GetMapping("/{id}/stream")
    fun streamPdf(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val doc = documentRepository.findById(id)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다.")

        val bytes = fileStorage.read(doc.storagePath)
            ?: throw IllegalStateException("파일을 찾을 수 없습니다: ${doc.storagePath}")

        val disposition = ContentDisposition.inline()
            .filename(doc.originalFileName, StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(bytes)
    }

    @GetMapping("/{id}/info")
    fun getInfo(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val doc = documentRepository.findById(id)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다.")

        return ResponseEntity.ok(
            mapOf(
                "id" to doc.id,
                "title" to doc.title,
                "pageCount" to doc.pageCount,
                "status" to doc.status,
            )
        )
    }
}
