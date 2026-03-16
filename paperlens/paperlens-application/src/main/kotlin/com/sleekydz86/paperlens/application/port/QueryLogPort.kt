package com.sleekydz86.paperlens.application.port

interface QueryLogPort {

    fun log(userId: Long, documentId: Long, question: String, answer: String, latencyMs: Long, modelName: String)
    fun getStats(): QueryLogStats
}

data class QueryLogStats(
    val totalQueries: Long,
    val avgLatencyMs: Double,
)