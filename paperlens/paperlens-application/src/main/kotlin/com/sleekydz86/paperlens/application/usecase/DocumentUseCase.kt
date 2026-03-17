package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.DocumentDetailResponse
import com.sleekydz86.paperlens.application.dto.DocumentResponse
import com.sleekydz86.paperlens.application.dto.DocumentUpdateRequest
import com.sleekydz86.paperlens.application.dto.PageResponse
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.application.port.PdfPort
import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

open class DocumentUseCase(
    private val documentRepository: DocumentRepositoryPort,
    private val chunkRepository: DocumentChunkRepositoryPort,
    private val fileStorage: FileStoragePort,
    private val processPort: DocumentProcessPort,
    private val pdfPort: PdfPort,
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
            throw IllegalArgumentException("유효하지 않은 PDF 파일입니다. 페이지 수를 읽을 수 없습니다.")
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
        runAfterCommit { processPort.processAsync(saved.id) }
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    open fun getDocuments(keyword: String?, docType: String?, page: Int, size: Int): PageResponse<DocumentResponse> {
        val result = documentRepository.searchDocuments(keyword, docType, page, size)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.pageNumber,
            size = result.pageSize,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    open fun getDocument(id: Long): DocumentDetailResponse {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다: $id")
        return document.toDetailResponse()
    }

    @Transactional
    open fun updateDocument(id: Long, request: DocumentUpdateRequest): DocumentDetailResponse {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다: $id")
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
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다: $id")
        documentRepository.save(document.softDelete())
    }

    @Transactional(readOnly = true)
    open fun downloadFile(id: Long): Pair<String, ByteArray> {
        val document = documentRepository.findById(id)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다: $id")
        val bytes = fileStorage.read(document.storagePath)
            ?: throw IllegalStateException("파일을 찾을 수 없습니다: ${document.storagePath}")
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
}
