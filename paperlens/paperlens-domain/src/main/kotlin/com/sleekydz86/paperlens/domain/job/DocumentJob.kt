package com.sleekydz86.paperlens.domain.job

import java.time.LocalDateTime

data class DocumentJob(
    val id: Long,
    val documentId: Long,
    val documentTitle: String,
    val jobType: DocumentJobType,
    val status: DocumentJobStatus,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime?,
    val errorMessage: String?,
)
