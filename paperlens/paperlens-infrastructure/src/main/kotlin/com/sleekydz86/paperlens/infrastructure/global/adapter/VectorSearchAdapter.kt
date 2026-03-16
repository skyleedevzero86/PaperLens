package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.dto.SimilarDocumentResult
import com.sleekydz86.paperlens.application.port.ChunkWithScore
import com.sleekydz86.paperlens.application.port.VectorSearchPort
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class VectorSearchAdapter(
    private val entityManager: EntityManager,
) : VectorSearchPort {

    override fun findRelevantChunks(documentId: Long, queryVector: FloatArray, limit: Int): List<ChunkWithScore> {
        val vectorStr = queryVector.joinToString(",", "[", "]")
        val sql = """
            SELECT dc.id, dc.content, dc.page_from, dc.page_to,
                   1 - (dc.embedding <=> ?::vector) AS similarity
            FROM document_chunks dc
            WHERE dc.document_id = ?
              AND dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> ?::vector
            LIMIT ?
        """.trimIndent()
        @Suppress("UNCHECKED_CAST")
        return entityManager.createNativeQuery(sql)
            .setParameter(1, vectorStr)
            .setParameter(2, documentId)
            .setParameter(3, vectorStr)
            .setParameter(4, limit)
            .resultList
            .filterIsInstance<Array<*>>()
            .map { row ->
                ChunkWithScore(
                    id = (row[0] as Number).toLong(),
                    content = row[1] as String,
                    pageFrom = (row[2] as Number).toInt(),
                    pageTo = (row[3] as Number).toInt(),
                    similarity = (row[4] as Number).toDouble(),
                )
            }
    }

    override fun findSimilarDocuments(documentId: Long, queryVector: FloatArray, limit: Int): List<SimilarDocumentResult> {
        val vectorStr = queryVector.joinToString(",", "[", "]")
        val sql = """
            SELECT d.id, d.title, d.summary_short,
                   AVG(1 - (dc.embedding <=> ?::vector)) AS avg_similarity
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.deleted_at IS NULL
              AND d.id != ?
              AND dc.embedding IS NOT NULL
            GROUP BY d.id, d.title, d.summary_short
            ORDER BY avg_similarity DESC
            LIMIT ?
        """.trimIndent()
        @Suppress("UNCHECKED_CAST")
        return entityManager.createNativeQuery(sql)
            .setParameter(1, vectorStr)
            .setParameter(2, documentId)
            .setParameter(3, limit)
            .resultList
            .filterIsInstance<Array<*>>()
            .map { row ->
                SimilarDocumentResult(
                    documentId = (row[0] as Number).toLong(),
                    title = row[1] as String,
                    summaryShort = row[2] as? String,
                    similarity = (row[3] as Number).toDouble(),
                )
            }
    }
}