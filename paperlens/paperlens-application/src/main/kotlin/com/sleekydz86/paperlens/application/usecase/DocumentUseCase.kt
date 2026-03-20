package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.DocumentDetailResponse
import com.sleekydz86.paperlens.application.dto.DocumentResponse
import com.sleekydz86.paperlens.application.dto.DocumentUpdateRequest
import com.sleekydz86.paperlens.application.dto.PageResponse
import com.sleekydz86.paperlens.application.port.AiPort
import com.sleekydz86.paperlens.application.port.DocumentJobPort
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.application.port.PdfPort
import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import java.util.UUID

open class DocumentUseCase(
    private val documentRepository: DocumentRepositoryPort,
    private val chunkRepository: DocumentChunkRepositoryPort,
    private val fileStorage: FileStoragePort,
    private val processPort: DocumentProcessPort,
    private val pdfPort: PdfPort,
    private val documentJobPort: DocumentJobPort,
    private val aiPort: AiPort,
) {

    @Transactional
    open fun uploadDocument(
        fileBytes: ByteArray,
        originalFileName: String,
        title: String,
        description: String?,
        userId: Long,
    ): DocumentResponse {
        val pageCount = pdfPort.getPageCount(fileBytes)
        if (pageCount <= 0) {
            throw IllegalArgumentException("?좏슚?섏? ?딆? PDF ?뚯씪?낅땲?? ?섏씠吏 ?섎? ?쎌쓣 ???놁뒿?덈떎.")
        }
        val fileName = "${UUID.randomUUID()}_$originalFileName"
        val storagePath = fileStorage.save(fileBytes, fileName)

        val document = Document(
            id = 0L,
            title = title,
            description = description,
            originalFileName = originalFileName,
            storagePath = storagePath,
            mimeType = "application/pdf",
            pageCount = pageCount,
            fileSize = fileBytes.size.toLong(),
            documentType = null,
            summaryShort = null,
            summaryLong = null,
            status = DocumentStatus.PENDING,
            createdBy = userId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            deletedAt = null,
            tagNames = emptyList(),
        )
        val saved = documentRepository.save(document)
        enqueueProcessingJobs(saved.id)
        runAfterCommit { processPort.processAsync(saved.id) }
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    open fun getDocuments(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int): PageResponse<DocumentResponse> {
        val result = documentRepository.searchDocuments(keyword, docType, tags, page, size)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.pageNumber,
            size = result.pageSize,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    open fun getAvailableTags(): List<String> = documentRepository.findAllTagNames()

    @Transactional
    open fun getDocument(id: Long): DocumentDetailResponse {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("臾몄꽌瑜?李얠쓣 ???놁뒿?덈떎: $id")
        return ensureSummary(document).toDetailResponse()
    }

    @Transactional
    open fun updateDocument(id: Long, request: DocumentUpdateRequest): DocumentDetailResponse {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("臾몄꽌瑜?李얠쓣 ???놁뒿?덈떎: $id")
        val updated = document.copy(
            title = request.title ?: document.title,
            description = request.description ?: document.description,
            tagNames = request.tags ?: document.tagNames,
            updatedAt = LocalDateTime.now(),
        )
        return documentRepository.save(updated).toDetailResponse()
    }

    @Transactional
    open fun deleteDocument(id: Long) {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("臾몄꽌瑜?李얠쓣 ???놁뒿?덈떎: $id")
        documentRepository.save(document.softDelete())
    }

    @Transactional(readOnly = true)
    open fun downloadFile(id: Long): Pair<String, ByteArray> {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("臾몄꽌瑜?李얠쓣 ???놁뒿?덈떎: $id")
        val bytes = fileStorage.read(document.storagePath)
            ?: throw IllegalStateException("?뚯씪??李얠쓣 ???놁뒿?덈떎: ${document.storagePath}")
        return Pair(document.originalFileName, bytes)
    }

    private fun runAfterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            }
        )
    }

    private fun enqueueProcessingJobs(documentId: Long) {
        DEFAULT_JOB_TYPES.forEach { jobType ->
            documentJobPort.enqueue(documentId, jobType)
        }
    }

    private fun ensureSummary(document: Document): Document {
        val needsSummary = document.summaryShort.isNullOrBlank() || document.summaryLong.isNullOrBlank()
        val needsDocType = document.documentType.isNullOrBlank()
        if (!needsSummary && !needsDocType) return document

        val chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(document.id)
        val combinedText = chunks.joinToString("\n") { it.content }.trim()
        if (combinedText.isBlank()) return document

        val summary = aiPort.summarizeText(combinedText)
        val documentType = if (needsDocType) aiPort.classifyDocumentType(combinedText) else document.documentType
        return documentRepository.save(
            document.withSummaries(summary.short, summary.long, documentType)
        )
    }

    private fun Document.toResponse() = DocumentResponse(
        id = id,
        title = title,
        description = description,
        originalFileName = originalFileName,
        pageCount = pageCount,
        fileSize = fileSize,
        documentType = documentType,
        summaryShort = summaryShort,
        status = status,
        tags = tagNames,
        createdAt = createdAt,
    )

    private fun Document.toDetailResponse() = DocumentDetailResponse(
        id = id,
        title = title,
        description = description,
        originalFileName = originalFileName,
        pageCount = pageCount,
        fileSize = fileSize,
        documentType = documentType,
        summaryShort = summaryShort,
        summaryLong = summaryLong,
        status = status,
        tags = tagNames,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private companion object {
        val DEFAULT_JOB_TYPES = listOf(
            DocumentJobType.PARSE,
            DocumentJobType.SUMMARY,
            DocumentJobType.EMBED,
        )
    }
}
