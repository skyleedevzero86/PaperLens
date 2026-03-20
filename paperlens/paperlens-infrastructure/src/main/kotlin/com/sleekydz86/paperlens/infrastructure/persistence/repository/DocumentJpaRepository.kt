package com.sleekydz86.paperlens.infrastructure.persistence.repository

import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentEntity
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentTagEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
interface DocumentJpaRepository : JpaRepository<DocumentEntity, Long> {

    @Query("""
        SELECT d FROM DocumentEntity d
        WHERE (:pattern IS NULL OR
               LOWER(d.title) LIKE :pattern OR
               LOWER(d.description) LIKE :pattern)
        AND (:docType IS NULL OR d.documentType = :docType)
        AND (:hasTags = false OR EXISTS (
            SELECT 1 FROM DocumentTagEntity dt
            WHERE dt.document = d AND dt.tagName IN :tags
        ))
        ORDER BY d.createdAt DESC
    """, countQuery = """
        SELECT COUNT(DISTINCT d.id) FROM DocumentEntity d
        WHERE (:pattern IS NULL OR
               LOWER(d.title) LIKE :pattern OR
               LOWER(d.description) LIKE :pattern)
        AND (:docType IS NULL OR d.documentType = :docType)
        AND (:hasTags = false OR EXISTS (
            SELECT 1 FROM DocumentTagEntity dt
            WHERE dt.document = d AND dt.tagName IN :tags
        ))
    """)
    fun searchDocuments(
        pattern: String?,
        docType: String?,
        hasTags: Boolean,
        tags: List<String>,
        pageable: Pageable
    ): Page<DocumentEntity>

    @Query("SELECT DISTINCT dt.tagName FROM DocumentTagEntity dt ORDER BY dt.tagName")
    fun findAllTagNames(): List<String>

    @EntityGraph(attributePaths = ["tags"])
    override fun findById(id: Long): java.util.Optional<DocumentEntity>

    @EntityGraph(attributePaths = ["tags"])
    fun findByStatus(status: DocumentStatus): List<DocumentEntity>
    fun countByStatus(status: DocumentStatus): Long
}
