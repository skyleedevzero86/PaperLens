package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.DocumentChunkMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentChunkJpaRepository
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DocumentChunkRepositoryAdapter(
    private val chunkJpaRepository: DocumentChunkJpaRepository,
    private val documentJpaRepository: DocumentJpaRepository,
    private val entityManager: EntityManager,
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

    @Transactional
    override fun updateEmbedding(chunkId: Long, embedding: FloatArray) {
        val vector = embedding.joinToString(",", "[", "]")
        entityManager.createNativeQuery(
            """
            UPDATE document_chunks
            SET embedding = ?::vector
            WHERE id = ?
            """.trimIndent()
        )
            .setParameter(1, vector)
            .setParameter(2, chunkId)
            .executeUpdate()
    }
}
