package com.sleekydz86.paperlens.infrastructure.persistence.repository

import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
interface DocumentJpaRepository : JpaRepository<DocumentEntity, Long> {

    @Query("""
        SELECT d FROM DocumentEntity d
        WHERE (:pattern IS NULL OR
               LOWER(d.title) LIKE :pattern OR
               LOWER(d.description) LIKE :pattern)
        AND (:docType IS NULL OR d.documentType = :docType)
        ORDER BY d.createdAt DESC
    """)
    fun searchDocuments(pattern: String?, docType: String?, pageable: Pageable): Page<DocumentEntity>

    fun findByStatus(status: DocumentStatus): List<DocumentEntity>
    fun countByStatus(status: DocumentStatus): Long
}