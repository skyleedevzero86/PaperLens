package com.sleekydz86.paperlens.application.dto

data class AdminStatsResponse(
    val totalDocuments: Long,
    val indexedDocuments: Long,
    val pendingDocuments: Long,
    val processingDocuments: Long,
    val failedDocuments: Long,
    val indexingRate: Double,
    val totalQueries: Long,
    val avgLatencyMs: Double,
)

data class DocumentJobResponse(
    val id: Long,
    val documentId: Long,
    val documentTitle: String,
    val jobType: String,
    val status: String,
    val startedAt: String,
    val finishedAt: String?,
    val errorMessage: String?,
)

data class CacheMetricResponse(
    val cacheName: String,
    val hits: Long,
    val misses: Long,
    val puts: Long,
    val evictions: Long,
    val errors: Long,
    val hitRate: Double,
)

data class CacheStatsResponse(
    val enabled: Boolean,
    val metrics: List<CacheMetricResponse>,
)

data class SessionInfoResponse(
    val sessionId: String,
    val principal: String?,
    val displayName: String?,
    val role: String?,
    val createdAt: String,
    val lastAccessedAt: String,
    val maxInactiveIntervalSeconds: Int,
)
