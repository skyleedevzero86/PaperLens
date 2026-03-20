package com.sleekydz86.paperlens.infrastructure.persistence.mapper

import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.infrastructure.global.text.TextSanitizer
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentChunkEntity
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentEntity

object DocumentChunkMapper {

    fun toDomain(e: DocumentChunkEntity): DocumentChunk = DocumentChunk(
        id = e.id,
        documentId = e.document.id,
        pageFrom = e.pageFrom,
        pageTo = e.pageTo,
        chunkIndex = e.chunkIndex,
        content = TextSanitizer.sanitize(e.content),
        tokenCount = e.tokenCount,
        embedding = e.embedding,
        createdAt = e.createdAt,
    )

    fun toEntity(d: DocumentChunk, document: DocumentEntity): DocumentChunkEntity = DocumentChunkEntity(
        id = d.id,
        document = document,
        pageFrom = d.pageFrom,
        pageTo = d.pageTo,
        chunkIndex = d.chunkIndex,
        content = TextSanitizer.sanitize(d.content),
        tokenCount = d.tokenCount,
        embedding = d.embedding,
        createdAt = d.createdAt,
    )
}