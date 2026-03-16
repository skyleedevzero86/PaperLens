package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import com.sleekydz86.paperlens.domain.shared.PageResult
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.DocumentMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class DocumentRepositoryAdapter(
    private val jpaRepository: DocumentJpaRepository,
) : DocumentRepositoryPort {

    override fun save(document: Document): Document {
        val entity = if (document.id == 0L) {
            DocumentMapper.toEntity(document)
        } else {
            val existing = jpaRepository.findById(document.id).orElse(null)
                ?: return DocumentMapper.toDomain(jpaRepository.save(DocumentMapper.toEntity(document)))
            DocumentMapper.updateEntity(existing, document)
            existing
        }
        val saved = jpaRepository.save(entity)
        return DocumentMapper.toDomain(saved)
    }

    override fun findById(id: Long): Document? =
        jpaRepository.findById(id).map { DocumentMapper.toDomain(it) }.orElse(null)

    override fun searchDocuments(keyword: String?, docType: String?, page: Int, size: Int): PageResult<Document> {
        val pageable = PageRequest.of(page, size)
        val result = jpaRepository.searchDocuments(keyword, docType, pageable)
        return PageResult(
            content = result.content.map { DocumentMapper.toDomain(it) },
            pageNumber = result.number,
            pageSize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    override fun findByStatus(status: DocumentStatus): List<Document> =
        jpaRepository.findByStatus(status).map { DocumentMapper.toDomain(it) }

    override fun countByStatus(status: DocumentStatus): Long = jpaRepository.countByStatus(status)
    override fun count(): Long = jpaRepository.count()
}
