package com.sleekydz86.paperlens.infrastructure.global.search

import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.dto.SearchResponse
import com.sleekydz86.paperlens.application.dto.SearchResult
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.application.strategy.SearchStrategy
import com.sleekydz86.paperlens.domain.search.SearchMode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class SemanticSearchStrategy(
    private val jdbcTemplate: JdbcTemplate,
    private val embeddingPort: EmbeddingPort,
) : SearchStrategy {

    override val mode: SearchMode = SearchMode.SEMANTIC

    override fun search(request: SearchRequest): SearchResponse {
        val docType = request.docType
        val embedding = embeddingPort.embed(request.query)
        val vectorStr = embedding.joinToString(",", "[", "]")
        val docTypeFilter = if (docType != null) "AND d.document_type = ?" else ""
        val countSql = """
            SELECT COUNT(*) FROM (
                SELECT d.id FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.deleted_at IS NULL AND dc.embedding IS NOT NULL $docTypeFilter
                GROUP BY d.id
            ) t
        """.trimIndent()
        val countArgs = if (docType != null) arrayOf(docType) else emptyArray<Any>()
        val totalElements = if (countArgs.isEmpty()) {
            jdbcTemplate.queryForObject(countSql, Long::class.java) ?: 0L
        } else {
            jdbcTemplate.queryForObject(countSql, Long::class.java, *countArgs) ?: 0L
        }
        val totalPages = if (request.size > 0) ((totalElements + request.size - 1) / request.size).toInt() else 0

        val sql = """
            WITH scored AS (
                SELECT d.id, d.title, d.summary_short, d.document_type,
                       MAX(1 - (dc.embedding <=> ?::vector)) AS score
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.deleted_at IS NULL AND dc.embedding IS NOT NULL $docTypeFilter
                GROUP BY d.id, d.title, d.summary_short, d.document_type
            )
            SELECT id, title, summary_short, document_type, score FROM scored
            ORDER BY score DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        val queryArgs = mutableListOf<Any>(vectorStr)
        if (docType != null) queryArgs.add(docType)
        queryArgs.addAll(listOf(request.size, request.page * request.size))
        val results = jdbcTemplate.query(sql, { rs, _ ->
            SearchResult(
                documentId = rs.getLong("id"),
                title = rs.getString("title"),
                summaryShort = rs.getString("summary_short"),
                documentType = rs.getString("document_type"),
                score = rs.getDouble("score"),
                highlights = emptyList(),
            )
        }, *queryArgs.toTypedArray())
        return SearchResponse(
            results = results,
            totalElements = totalElements,
            totalPages = totalPages,
            page = request.page,
            size = request.size,
            query = request.query,
            mode = mode,
        )
    }
}
