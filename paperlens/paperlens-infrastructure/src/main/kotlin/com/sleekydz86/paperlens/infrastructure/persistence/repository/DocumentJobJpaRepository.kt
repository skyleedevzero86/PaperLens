package com.sleekydz86.paperlens.infrastructure.persistence.repository

import com.sleekydz86.paperlens.domain.job.DocumentJobStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentJobEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentJobJpaRepository : JpaRepository<DocumentJobEntity, Long> {

    @EntityGraph(attributePaths = ["document"])
    fun findByDocument_IdOrderByStartedAtDesc(documentId: Long): List<DocumentJobEntity>

    @EntityGraph(attributePaths = ["document"])
    fun findAllByOrderByStartedAtDesc(pageable: Pageable): List<DocumentJobEntity>

    @EntityGraph(attributePaths = ["document"])
    fun findFirstByDocument_IdAndJobTypeAndStatusOrderByStartedAtDesc(
        documentId: Long,
        jobType: DocumentJobType,
        status: DocumentJobStatus,
    ): DocumentJobEntity?

    @EntityGraph(attributePaths = ["document"])
    fun findByDocument_IdAndStatusOrderByStartedAtDesc(
        documentId: Long,
        status: DocumentJobStatus,
    ): List<DocumentJobEntity>
}
