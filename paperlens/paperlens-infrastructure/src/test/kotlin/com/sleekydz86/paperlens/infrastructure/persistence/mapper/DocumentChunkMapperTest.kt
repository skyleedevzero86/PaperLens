package com.sleekydz86.paperlens.infrastructure.persistence.mapper

import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DocumentChunkMapperTest {

    @Test
    fun `toEntity sanitizes invalid text before persistence`() {
        val now = LocalDateTime.now()
        val document = DocumentEntity(
            id = 1L,
            title = "test",
            description = null,
            originalFileName = "test.pdf",
            storagePath = "/tmp/test.pdf",
            mimeType = "application/pdf",
            pageCount = 1,
            fileSize = 10,
            documentType = null,
            summaryShort = null,
            summaryLong = null,
            status = DocumentStatus.PENDING,
            createdBy = 1L,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        val chunk = DocumentChunk(
            id = 0L,
            documentId = 1L,
            pageFrom = 1,
            pageTo = 1,
            chunkIndex = 0,
            content = "Hello\u0000World",
            tokenCount = 2,
            embedding = null,
            createdAt = now,
        )

        val entity = DocumentChunkMapper.toEntity(chunk, document)

        assertEquals("HelloWorld", entity.content)
    }
}