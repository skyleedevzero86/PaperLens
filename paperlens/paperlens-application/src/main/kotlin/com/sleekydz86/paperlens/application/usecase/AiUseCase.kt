package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.QaRequest
import com.sleekydz86.paperlens.application.dto.QaResponse
import com.sleekydz86.paperlens.application.dto.SimilarDocumentResult
import com.sleekydz86.paperlens.application.port.AiPort
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.application.port.QueryLogPort
import com.sleekydz86.paperlens.application.port.VectorSearchPort
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort

class AiUseCase(
    private val aiPort: AiPort,
    private val embeddingPort: EmbeddingPort,
    private val vectorSearchPort: VectorSearchPort,
    private val documentRepository: DocumentRepositoryPort,
    private val queryLogPort: QueryLogPort,
) {

    fun answerQuestion(request: QaRequest): QaResponse {
        val document = documentRepository.findById(request.documentId)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다.")
        if (document.status != DocumentStatus.INDEXED) {
            return QaResponse(
                answer = "문서 인덱싱이 아직 완료되지 않았습니다. 잠시 후 다시 시도해주세요.",
                sources = emptyList(),
            )
        }

        val queryVector = embeddingPort.embed(request.question)
        val chunks = vectorSearchPort.findRelevantChunks(request.documentId, queryVector, 5)
        if (chunks.isEmpty()) {
            return QaResponse(
                answer = "관련 내용을 아직 찾지 못했습니다. 문서 인덱싱 상태를 확인해주세요.",
                sources = emptyList(),
            )
        }

        val context = chunks.joinToString("\n\n") { it.content }
        val answer = aiPort.answerQuestion(request.question, context)
        val sources = chunks.map { it.toChunkSource() }
        return QaResponse(answer = answer, sources = sources)
    }

    fun findSimilarDocuments(documentId: Long, limit: Int = 5): List<SimilarDocumentResult> {
        val doc = documentRepository.findById(documentId)
            ?: throw NoSuchElementException("문서를 찾을 수 없습니다.")
        if (doc.status != DocumentStatus.INDEXED) {
            return emptyList()
        }

        val queryText = doc.summaryShort ?: doc.title
        val queryVector = embeddingPort.embed(queryText)
        return vectorSearchPort.findSimilarDocuments(documentId, queryVector, limit)
    }

    fun logQuery(userId: Long, documentId: Long, question: String, answer: String, latencyMs: Long, modelName: String) {
        queryLogPort.log(userId, documentId, question, answer, latencyMs, modelName)
    }
}
