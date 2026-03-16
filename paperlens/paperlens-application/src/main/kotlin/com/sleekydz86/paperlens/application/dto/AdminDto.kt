package com.sleekydz86.paperlens.application.dto

data class AdminStatsResponse(
    val totalDocuments: Long,
    val indexedDocuments: Long,
    val pendingDocuments: Long,
    val failedDocuments: Long,
    val indexingRate: Double,
    val totalQueries: Long,
    val avgLatencyMs: Double,
)
