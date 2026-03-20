package com.sleekydz86.paperlens.domain.port

import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.domain.document.DocumentStatus
import com.sleekydz86.paperlens.domain.shared.PageResult

interface DocumentRepositoryPort {

    fun save(document: Document): Document
    fun findById(id: Long): Document?
    fun searchDocuments(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int): PageResult<Document>
    fun findAllTagNames(): List<String>
    fun findByStatus(status: DocumentStatus): List<Document>
    fun countByStatus(status: DocumentStatus): Long
    fun count(): Long
}
