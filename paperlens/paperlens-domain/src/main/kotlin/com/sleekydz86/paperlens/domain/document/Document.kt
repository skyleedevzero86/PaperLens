package com.sleekydz86.paperlens.domain.document

import java.time.LocalDateTime

data class Document(
    val id: Long,
    val title: String,
    val description: String?,
    val originalFileName: String,
    val storagePath: String,
    val mimeType: String,
    val pageCount: Int,
    val fileSize: Long,
    val documentType: String?,
    val summaryShort: String?,
    val summaryLong: String?,
    val status: DocumentStatus,
    val createdBy: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val tagNames: List<String>,
) {
    fun withStatus(newStatus: DocumentStatus): Document =
        copy(status = newStatus, updatedAt = LocalDateTime.now())

    fun withSummaries(short: String?, long: String?, docType: String?): Document =
        copy(summaryShort = short, summaryLong = long, documentType = docType, updatedAt = LocalDateTime.now())

    fun withTags(newTagNames: List<String>): Document =
        copy(tagNames = newTagNames, updatedAt = LocalDateTime.now())

    fun softDelete(): Document =
        copy(deletedAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

    val isDeleted: Boolean get() = deletedAt != null
}