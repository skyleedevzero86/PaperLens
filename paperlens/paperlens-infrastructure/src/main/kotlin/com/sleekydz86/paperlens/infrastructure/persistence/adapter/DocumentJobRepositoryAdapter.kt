package com.sleekydz86.paperlens.infrastructure.persistence.adapter

import com.sleekydz86.paperlens.application.port.DocumentJobPort
import com.sleekydz86.paperlens.domain.job.DocumentJob
import com.sleekydz86.paperlens.domain.job.DocumentJobStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentJobEntity
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJobJpaRepository
import com.sleekydz86.paperlens.infrastructure.persistence.repository.DocumentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class DocumentJobRepositoryAdapter(
    private val documentJobJpaRepository: DocumentJobJpaRepository,
    private val documentJpaRepository: DocumentJpaRepository,
) : DocumentJobPort {

    @Transactional
    override fun enqueue(documentId: Long, jobType: DocumentJobType): Long {
        val document = documentJpaRepository.getReferenceById(documentId)
        val saved = documentJobJpaRepository.save(
            DocumentJobEntity(
                document = document,
                jobType = jobType,
                status = DocumentJobStatus.PENDING,
                startedAt = LocalDateTime.now(),
            )
        )
        return saved.id
    }

    @Transactional
    override fun start(documentId: Long, jobType: DocumentJobType): Long {
        val pending = documentJobJpaRepository.findFirstByDocument_IdAndJobTypeAndStatusOrderByStartedAtDesc(
            documentId = documentId,
            jobType = jobType,
            status = DocumentJobStatus.PENDING,
        )
        if (pending != null) {
            pending.status = DocumentJobStatus.RUNNING
            pending.errorMessage = null
            return pending.id
        }

        val document = documentJpaRepository.getReferenceById(documentId)
        val saved = documentJobJpaRepository.save(
            DocumentJobEntity(
                document = document,
                jobType = jobType,
                status = DocumentJobStatus.RUNNING,
                startedAt = LocalDateTime.now(),
            )
        )
        return saved.id
    }

    @Transactional
    override fun complete(jobId: Long) {
        val job = documentJobJpaRepository.findById(jobId).orElseThrow()
        job.status = DocumentJobStatus.SUCCESS
        job.finishedAt = LocalDateTime.now()
        job.errorMessage = null
    }

    @Transactional
    override fun fail(jobId: Long, errorMessage: String?) {
        val job = documentJobJpaRepository.findById(jobId).orElseThrow()
        job.status = DocumentJobStatus.FAILED
        job.finishedAt = LocalDateTime.now()
        job.errorMessage = errorMessage?.take(2000)
    }

    @Transactional
    override fun failPending(documentId: Long, errorMessage: String?) {
        documentJobJpaRepository.findByDocument_IdAndStatusOrderByStartedAtDesc(
            documentId = documentId,
            status = DocumentJobStatus.PENDING,
        ).forEach { job ->
            job.status = DocumentJobStatus.FAILED
            job.finishedAt = LocalDateTime.now()
            job.errorMessage = errorMessage?.take(2000)
        }
    }

    override fun getByDocumentId(documentId: Long): List<DocumentJob> =
        documentJobJpaRepository.findByDocument_IdOrderByStartedAtDesc(documentId).map(::toDomain)

    override fun getRecent(limit: Int): List<DocumentJob> =
        documentJobJpaRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit)).map(::toDomain)

    private fun toDomain(entity: DocumentJobEntity) = DocumentJob(
        id = entity.id,
        documentId = entity.document.id,
        documentTitle = entity.document.title,
        jobType = entity.jobType,
        status = entity.status,
        startedAt = entity.startedAt,
        finishedAt = entity.finishedAt,
        errorMessage = entity.errorMessage,
    )
}
