package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import com.sleekydz86.paperlens.domain.shared.PageResult
import com.sleekydz86.paperlens.infrastructure.persistence.mapper.DocumentMapper
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJpaRepository
import org.hibernate.Hibernate
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DocumentRepositoryAdapter(
    private val jpaRepository: DocumentJpaRepository,
) : DocumentRepositoryPort {

    @Transactional
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

    @Transactional(readOnly = true)
    override fun findById(id: Long): Document? =
        jpaRepository.findById(id).map {
            Hibernate.initialize(it.tags)
            DocumentMapper.toDomain(it)
        }.orElse(null)

    @Transactional(readOnly = true)
    override fun searchDocuments(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int): PageResult<Document> {
        val pageable = PageRequest.of(page, size)
        val pattern = keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { "%${it.lowercase()}%" }
        val normalizedTags = tags.map(String::trim).filter(String::isNotEmpty).distinct()
        val result = jpaRepository.searchDocuments(
            pattern = pattern,
            docType = docType,
            hasTags = normalizedTags.isNotEmpty(),
            tags = if (normalizedTags.isNotEmpty()) normalizedTags else listOf("__NO_TAG_FILTER__"),
            pageable = pageable,
        )
        result.content.forEach { Hibernate.initialize(it.tags) }
        return PageResult(
            content = result.content.map { DocumentMapper.toDomain(it) },
            pageNumber = result.number,
            pageSize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    override fun findAllTagNames(): List<String> = jpaRepository.findAllTagNames()

    @Transactional(readOnly = true)
    override fun findByStatus(status: DocumentStatus): List<Document> =
        jpaRepository.findByStatus(status).map {
            Hibernate.initialize(it.tags)
            DocumentMapper.toDomain(it)
        }

    @Transactional(readOnly = true)
    override fun countByStatus(status: DocumentStatus): Long = jpaRepository.countByStatus(status)
    @Transactional(readOnly = true)
    override fun count(): Long = jpaRepository.count()
}
