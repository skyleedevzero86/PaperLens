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
class HybridSearchStrategy(
    private val jdbcTemplate: JdbcTemplate,
    private val embeddingPort: EmbeddingPort,
) : SearchStrategy {

    override val mode: SearchMode = SearchMode.HYBRID

    override fun search(request: SearchRequest): SearchResponse {
        val docType = request.docType
        val tags = SearchSqlSupport.normalizeTags(request.tags)
        val embedding = embeddingPort.embed(request.query)
        val vectorStr = embedding.joinToString(",", "[", "]")
        val docTypeParam = if (docType != null) "AND d.document_type = ?" else ""
        val tagFilter = SearchSqlSupport.buildTagFilter("d", tags)

        val countSql = """
            SELECT COUNT(*) FROM documents d
            WHERE d.deleted_at IS NULL
              AND (?::text IS NULL OR d.document_type = ?)
              $tagFilter
        """.trimIndent()
        val countArgs = mutableListOf<Any?>(docType, docType)
        SearchSqlSupport.addTags(countArgs, tags)
        val totalElements = jdbcTemplate.queryForObject(countSql, Long::class.java, *countArgs.toTypedArray()) ?: 0L
        val totalPages = if (request.size > 0) ((totalElements + request.size - 1) / request.size).toInt() else 0

        val sql = """
            WITH keyword AS (
                SELECT d.id,
                       ts_rank(to_tsvector('simple', coalesce(d.title,'') || ' ' || coalesce(d.description,'')),
                               plainto_tsquery('simple', ?)) * 0.5 AS lex_score
                FROM documents d
                WHERE d.deleted_at IS NULL $docTypeParam
            ),
            vector AS (
                SELECT dc.document_id AS id,
                       MAX(1 - (dc.embedding <=> ?::vector)) * 0.35 AS vec_score
                FROM document_chunks dc
                WHERE dc.embedding IS NOT NULL
                GROUP BY dc.document_id
            ),
            trgm AS (
                SELECT d.id,
                       similarity(d.title, ?) * 0.1 AS trgm_score
                FROM documents d
                WHERE d.deleted_at IS NULL $docTypeParam
            )
            SELECT d.id, d.title, d.summary_short, d.document_type,
                   ${SearchSqlSupport.tagsProjection("d")},
                   COALESCE(k.lex_score,0) + COALESCE(v.vec_score,0) + COALESCE(t.trgm_score,0) AS score
            FROM documents d
            LEFT JOIN keyword k ON d.id = k.id
            LEFT JOIN vector v ON d.id = v.id
            LEFT JOIN trgm t ON d.id = t.id
            WHERE d.deleted_at IS NULL $docTypeParam
              $tagFilter
              AND (COALESCE(k.lex_score,0) + COALESCE(v.vec_score,0) + COALESCE(t.trgm_score,0)) > 0
            ORDER BY score DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val queryArgs = mutableListOf<Any?>(request.query)
        if (docType != null) queryArgs.add(docType)
        queryArgs.add(vectorStr)
        queryArgs.add(request.query)
        if (docType != null) queryArgs.add(docType)
        if (docType != null) queryArgs.add(docType)
        SearchSqlSupport.addTags(queryArgs, tags)
        queryArgs.add(request.size)
        queryArgs.add(request.page * request.size)

        val results = jdbcTemplate.query(sql, { rs, _ ->
            SearchResult(
                documentId = rs.getLong("id"),
                title = rs.getString("title"),
                summaryShort = rs.getString("summary_short"),
                documentType = rs.getString("document_type"),
                tags = SearchSqlSupport.parseTags(rs.getString("tags")),
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
