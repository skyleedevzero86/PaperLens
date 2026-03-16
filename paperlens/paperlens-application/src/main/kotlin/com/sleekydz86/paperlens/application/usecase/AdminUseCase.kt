package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.AdminStatsResponse
import com.sleekydz86.paperlens.application.dto.DocumentResponse
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.QueryLogPort
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort

class AdminUseCase(
    private val documentRepository: DocumentRepositoryPort,
    private val queryLogPort: QueryLogPort,
    private val processPort: DocumentProcessPort,
) {

    fun getStats(): AdminStatsResponse {
        val total = documentRepository.count()
        val indexed = documentRepository.countByStatus(DocumentStatus.INDEXED)
        val pending = documentRepository.countByStatus(DocumentStatus.PENDING)
        val failed = documentRepository.countByStatus(DocumentStatus.FAILED)
        val logStats = queryLogPort.getStats()
        return AdminStatsResponse(
            totalDocuments = total,
            indexedDocuments = indexed,
            pendingDocuments = pending,
            failedDocuments = failed,
            indexingRate = if (total > 0) indexed * 100.0 / total else 0.0,
            totalQueries = logStats.totalQueries,
            avgLatencyMs = logStats.avgLatencyMs,
        )
    }

    fun reprocess(documentId: Long) {
        val doc = documentRepository.findById(documentId)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다: $documentId")
        documentRepository.save(doc.withStatus(DocumentStatus.PENDING))
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
}
