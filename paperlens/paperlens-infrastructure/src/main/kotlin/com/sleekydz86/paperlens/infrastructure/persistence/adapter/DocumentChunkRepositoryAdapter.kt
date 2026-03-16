package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.DocumentChunkMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentChunkJpaRepository
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJpaRepository
import org.springframework.stereotype.Component

@Component
class DocumentChunkRepositoryAdapter(
    private val chunkJpaRepository: DocumentChunkJpaRepository,
    private val documentJpaRepository: DocumentJpaRepository,
) : DocumentChunkRepositoryPort {

    override fun save(chunk: DocumentChunk): DocumentChunk {
        val document = documentJpaRepository.findById(chunk.documentId).orElseThrow()
        val entity = DocumentChunkMapper.toEntity(chunk, document)
        val saved = chunkJpaRepository.save(entity)
        return DocumentChunkMapper.toDomain(saved)
    }

    override fun saveAll(chunks: List<DocumentChunk>): List<DocumentChunk> {
        if (chunks.isEmpty()) return emptyList()
        val documentId = chunks.first().documentId
        val document = documentJpaRepository.findById(documentId).orElseThrow()
        val entities = chunks.map { DocumentChunkMapper.toEntity(it, document) }
        return chunkJpaRepository.saveAll(entities).map { DocumentChunkMapper.toDomain(it) }
    }

    override fun findByDocumentIdOrderByChunkIndex(documentId: Long): List<DocumentChunk> =
        chunkJpaRepository.findByDocumentIdOrderByChunkIndex(documentId).map { DocumentChunkMapper.toDomain(it) }

    override fun deleteByDocumentId(documentId: Long) {
        chunkJpaRepository.deleteByDocumentId(documentId)
    }

    override fun updateEmbedding(chunkId: Long, embedding: FloatArray) {
        val entity = chunkJpaRepository.findById(chunkId).orElseThrow()
        entity.embedding = embedding
        chunkJpaRepository.save(entity)
    }
}