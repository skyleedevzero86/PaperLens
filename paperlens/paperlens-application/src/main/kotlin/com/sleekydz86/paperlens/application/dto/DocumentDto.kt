package com.sleekydz86.paperlens.application.dto

import com.sleekydz86.paperlens.domain.document.DocumentStatus
import java.time.LocalDateTime

data class DocumentResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val originalFileName: String,
    val pageCount: Int,
    val fileSize: Long,
    val documentType: String?,
    val summaryShort: String?,
    val status: DocumentStatus,
    val tags: List<String>,
    val createdAt: LocalDateTime
)

data class DocumentDetailResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val originalFileName: String,
    val pageCount: Int,
    val fileSize: Long,
    val documentType: String?,
    val summaryShort: String?,
    val summaryLong: String?,
    val status: DocumentStatus,
    val tags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class DocumentUpdateRequest(
    val title: String?,
    val description: String?,
    val tags: List<String>?
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
