package com.sleekydz86.paperlens.application.dto

import com.sleekydz86.paperlens.domain.search.SearchMode

data class SearchRequest(
    val query: String,
    val mode: SearchMode = SearchMode.HYBRID,
    val docType: String? = null,
    val tags: List<String> = emptyList(),
    val page: Int = 0,
    val size: Int = 20
)

data class SearchResult(
    val documentId: Long,
    val title: String,
    val summaryShort: String?,
    val documentType: String?,
    val tags: List<String>,
    val score: Double,
    val highlights: List<String>
)

data class SearchResponse(
    val results: List<SearchResult>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val query: String,
    val mode: SearchMode
) {
    val total: Int get() = results.size
}
