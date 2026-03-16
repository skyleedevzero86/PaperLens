package com.sleekydz86.paperlens.application.dto

data class SummaryResult(
    val short: String,
    val long: String,
    val keywords: List<String>
)

data class QaRequest(
    val question: String,
    val documentId: Long
)

data class QaResponse(
    val answer: String,
    val sources: List<ChunkSource>
)

data class ChunkSource(
    val chunkId: Long,
    val pageFrom: Int,
    val pageTo: Int,
    val excerpt: String
)

data class SimilarDocumentResult(
    val documentId: Long,
    val title: String,
    val summaryShort: String?,
    val similarity: Double
)