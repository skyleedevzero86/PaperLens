package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.AiPort
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.domain.document.DocumentChunk
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
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
) : DocumentProcessPort {

    @Async
    override fun processAsync(documentId: Long) {
        val document = documentRepository.findById(documentId) ?: return
        try {
            documentRepository.save(document.withStatus(DocumentStatus.PROCESSING))
            chunkRepository.deleteByDocumentId(documentId)
            val bytes = fileStorage.read(document.storagePath) ?: throw IllegalStateException("파일을 찾을 수 없습니다.")
            val chunks = extractAndChunk(documentId, bytes)
            chunkRepository.saveAll(chunks)

            val allText = chunks.joinToString("\n") { it.content }
            if (allText.isNotBlank()) {
                val summary = aiPort.summarizeText(allText)
                val docType = aiPort.classifyDocumentType(allText)
                documentRepository.save(
                    document.withStatus(DocumentStatus.INDEXED)
                        .withSummaries(summary.short, summary.long, docType)
                )
            } else {
                documentRepository.save(document.withStatus(DocumentStatus.INDEXED))
            }

            val savedChunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId)
            savedChunks.forEach { chunk ->
                val embedding = embeddingPort.embed(chunk.content)
                chunkRepository.updateEmbedding(chunk.id, embedding)
            }
        } catch (e: Exception) {
            documentRepository.save(document.withStatus(DocumentStatus.FAILED))
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
                val text = stripper.getText(pdf).trim()
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
        if (text.length <= maxLength) return listOf(text)
        return text.chunked(maxLength)
    }
}
