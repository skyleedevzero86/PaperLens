package com.sleekydz86.paperlens.application.port

import com.sleekydz86.paperlens.domain.job.DocumentJob
import com.sleekydz86.paperlens.domain.job.DocumentJobType

interface DocumentJobPort {

    fun enqueue(documentId: Long, jobType: DocumentJobType): Long
    fun start(documentId: Long, jobType: DocumentJobType): Long
    fun complete(jobId: Long)
    fun fail(jobId: Long, errorMessage: String?)
    fun failPending(documentId: Long, errorMessage: String?)
    fun getByDocumentId(documentId: Long): List<DocumentJob>
    fun getRecent(limit: Int): List<DocumentJob>
}
