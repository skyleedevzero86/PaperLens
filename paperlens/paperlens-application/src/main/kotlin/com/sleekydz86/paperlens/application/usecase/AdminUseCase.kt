package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.AdminStatsResponse
import com.sleekydz86.paperlens.application.dto.DocumentJobResponse
import com.sleekydz86.paperlens.application.dto.DocumentResponse
import com.sleekydz86.paperlens.application.port.DocumentJobPort
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.QueryLogPort
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort

class AdminUseCase(
    private val documentRepository: DocumentRepositoryPort,
    private val queryLogPort: QueryLogPort,
    private val processPort: DocumentProcessPort,
    private val documentJobPort: DocumentJobPort,
) {

    fun getStats(): AdminStatsResponse {
        val total = documentRepository.count()
        val indexed = documentRepository.countByStatus(DocumentStatus.INDEXED)
        val pending = documentRepository.countByStatus(DocumentStatus.PENDING)
        val processing = documentRepository.countByStatus(DocumentStatus.PROCESSING)
        val failed = documentRepository.countByStatus(DocumentStatus.FAILED)
        val logStats = queryLogPort.getStats()
        return AdminStatsResponse(
            totalDocuments = total,
            indexedDocuments = indexed,
            pendingDocuments = pending,
            processingDocuments = processing,
            failedDocuments = failed,
            indexingRate = if (total > 0) indexed * 100.0 / total else 0.0,
            totalQueries = logStats.totalQueries,
            avgLatencyMs = logStats.avgLatencyMs,
        )
    }

    fun reprocess(documentId: Long) {
        val doc = documentRepository.findById(documentId)
            ?: throw NoSuchElementException("臾몄꽌瑜?李얠쓣 ???놁뒿?덈떎: $documentId")
        documentRepository.save(doc.withStatus(DocumentStatus.PENDING))
        DEFAULT_JOB_TYPES.forEach { jobType ->
            documentJobPort.enqueue(documentId, jobType)
        }
        processPort.processAsync(documentId)
    }

    fun getFailedDocuments(): List<DocumentResponse> =
        documentRepository.findByStatus(DocumentStatus.FAILED).map { doc ->
            DocumentResponse(
                id = doc.id,
                title = doc.title,
                description = doc.description,
                originalFileName = doc.originalFileName,
                pageCount = doc.pageCount,
                fileSize = doc.fileSize,
                documentType = doc.documentType,
                summaryShort = doc.summaryShort,
                status = doc.status,
                tags = doc.tagNames,
                createdAt = doc.createdAt,
            )
        }

    fun getDocumentJobs(documentId: Long): List<DocumentJobResponse> =
        documentJobPort.getByDocumentId(documentId).map(::toJobResponse)

    fun getRecentDocumentJobs(limit: Int): List<DocumentJobResponse> =
        documentJobPort.getRecent(limit.coerceIn(1, 200)).map(::toJobResponse)

    private fun toJobResponse(job: com.sleekydz86.paperlens.domain.job.DocumentJob) =
        DocumentJobResponse(
            id = job.id,
            documentId = job.documentId,
            documentTitle = job.documentTitle,
            jobType = job.jobType.name,
            status = job.status.name,
            startedAt = job.startedAt.toString(),
            finishedAt = job.finishedAt?.toString(),
            errorMessage = job.errorMessage,
        )

    private companion object {
        val DEFAULT_JOB_TYPES = listOf(
            DocumentJobType.PARSE,
            DocumentJobType.SUMMARY,
            DocumentJobType.EMBED,
        )
    }
}
