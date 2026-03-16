package com.sleekydz86.paperlens.application.port

import com.sleekydz86.paperlens.application.dto.ChunkSource
import com.sleekydz86.paperlens.application.dto.SimilarDocumentResult

interface VectorSearchPort {

    fun findRelevantChunks(documentId: Long, queryVector: FloatArray, limit: Int): List<ChunkWithScore>
    fun findSimilarDocuments(documentId: Long, queryVector: FloatArray, limit: Int): List<SimilarDocumentResult>
}

data class ChunkWithScore(
    val id: Long,
    val content: String,
    val pageFrom: Int,
    val pageTo: Int,
    val similarity: Double
) {
    fun toChunkSource(excerptLength: Int = 200): ChunkSource =
        ChunkSource(chunkId = id, pageFrom = pageFrom, pageTo = pageTo, excerpt = content.take(excerptLength))
}
