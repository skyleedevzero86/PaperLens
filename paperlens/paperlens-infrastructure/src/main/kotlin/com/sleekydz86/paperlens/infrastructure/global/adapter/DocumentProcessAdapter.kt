package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.AiPort
import com.sleekydz86.paperlens.application.port.DocumentJobPort
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import com.sleekydz86.paperlens.infrastructure.global.text.TextSanitizer
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DocumentProcessAdapter(
    private val documentRepository: DocumentRepositoryPort,
    private val chunkRepository: DocumentChunkRepositoryPort,
    private val fileStorage: FileStoragePort,
    private val aiPort: AiPort,
    private val embeddingPort: EmbeddingPort,
    private val documentJobPort: DocumentJobPort,
    private val redisCacheService: RedisCacheService,
) : DocumentProcessPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    override fun processAsync(documentId: Long) {
        val document = documentRepository.findById(documentId) ?: return
        try {
            documentRepository.save(document.withStatus(DocumentStatus.PROCESSING))
            chunkRepository.deleteByDocumentId(documentId)

            val savedChunks = runTrackedJob(documentId, DocumentJobType.PARSE) {
                val bytes = fileStorage.read(document.storagePath)
                    ?: throw IllegalStateException("File not found: ${document.storagePath}")
                val chunks = extractAndChunk(documentId, bytes)
                chunkRepository.saveAll(chunks)
            }

            val allText = savedChunks.joinToString("\n") { it.content }
            val summaryResult = runTrackedJob(documentId, DocumentJobType.SUMMARY) {
                if (allText.isBlank()) {
                    SummaryResult(short = null, long = null, documentType = null)
                } else {
                    val summary = aiPort.summarizeText(allText)
                    val docType = aiPort.classifyDocumentType(allText)
                    SummaryResult(summary.short, summary.long, docType)
                }
            }

            runTrackedJob(documentId, DocumentJobType.EMBED) {
                savedChunks.forEach { chunk ->
                    val embedding = embeddingPort.embed(chunk.content)
                    chunkRepository.updateEmbedding(chunk.id, embedding)
                }
            }

            documentRepository.save(
                document.withStatus(DocumentStatus.INDEXED)
                    .withSummaries(summaryResult.short, summaryResult.long, summaryResult.documentType)
            )
        } catch (e: Exception) {
            logger.error("Document processing failed: documentId={}", documentId, e)
            documentJobPort.failPending(documentId, "Skipped because a previous step failed: ${e.message ?: "unknown error"}")
            documentRepository.save(document.withStatus(DocumentStatus.FAILED))
        } finally {
            redisCacheService.evictDocumentCaches()
        }
    }

    private fun extractAndChunk(documentId: Long, fileBytes: ByteArray): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        Loader.loadPDF(fileBytes).use { pdf ->
            val stripper = PDFTextStripper()
            val totalPages = pdf.numberOfPages
            var chunkIndex = 0
            var pageFrom = 1
            while (pageFrom <= totalPages) {
                val pageTo = minOf(pageFrom + 2, totalPages)
                stripper.startPage = pageFrom
                stripper.endPage = pageTo
                val rawText = stripper.getText(pdf)
                val sanitizedText = TextSanitizer.sanitize(rawText)
                if (rawText.length != sanitizedText.length) {
                    logger.warn(
                        "Removed invalid control characters from extracted text: documentId={}, pageFrom={}, pageTo={}",
                        documentId,
                        pageFrom,
                        pageTo,
                    )
                }
                val text = sanitizedText.trim()
                if (text.isNotBlank()) {
                    splitText(text, 1000).forEach { subText ->
                        chunks.add(
                            DocumentChunk(
                                id = 0L,
                                documentId = documentId,
                                pageFrom = pageFrom,
                                pageTo = pageTo,
                                chunkIndex = chunkIndex++,
                                content = subText,
                                tokenCount = (subText.length / 4).coerceAtLeast(1),
                                embedding = null,
                                createdAt = LocalDateTime.now(),
                            )
                        )
                    }
                }
                pageFrom = pageTo + 1
            }
        }
        return chunks
    }

    private fun splitText(text: String, maxLength: Int): List<String> {
        val sanitized = TextSanitizer.sanitize(text).trim()
        if (sanitized.isBlank()) return emptyList()
        if (sanitized.length <= maxLength) return listOf(sanitized)
        return sanitized.chunked(maxLength)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun <T> runTrackedJob(documentId: Long, jobType: DocumentJobType, action: () -> T): T {
        val jobId = documentJobPort.start(documentId, jobType)
        return try {
            action().also { documentJobPort.complete(jobId) }
        } catch (e: Exception) {
            documentJobPort.fail(jobId, e.message)
            throw e
        }
    }

    private data class SummaryResult(
        val short: String?,
        val long: String?,
        val documentType: String?,
    )
}