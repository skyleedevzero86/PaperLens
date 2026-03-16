package com.sleekydz86.paperlens.domain.port

import com.sleekydz86.paperlens.domain.document.DocumentChunk

interface DocumentChunkRepositoryPort {

    fun save(chunk: DocumentChunk): DocumentChunk
    fun saveAll(chunks: List<DocumentChunk>): List<DocumentChunk>
    fun findByDocumentIdOrderByChunkIndex(documentId: Long): List<DocumentChunk>
    fun deleteByDocumentId(documentId: Long)
    fun updateEmbedding(chunkId: Long, embedding: FloatArray)
}
