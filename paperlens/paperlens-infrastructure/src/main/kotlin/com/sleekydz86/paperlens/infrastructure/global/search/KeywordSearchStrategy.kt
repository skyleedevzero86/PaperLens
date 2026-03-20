package com.sleekydz86.paperlens.infrastructure.global.search

import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.dto.SearchResponse
import com.sleekydz86.paperlens.application.dto.SearchResult
import com.sleekydz86.paperlens.application.strategy.SearchStrategy
import com.sleekydz86.paperlens.domain.search.SearchMode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class KeywordSearchStrategy(
    private val jdbcTemplate: JdbcTemplate,
) : SearchStrategy {

    override val mode: SearchMode = SearchMode.KEYWORD

    override fun search(request: SearchRequest): SearchResponse {
        val tags = SearchSqlSupport.normalizeTags(request.tags)
        val tagFilter = SearchSqlSupport.buildTagFilter("d", tags)

        val baseWhere = """
            d.deleted_at IS NULL
              AND to_tsvector('simple', coalesce(d.title,'') || ' ' || coalesce(d.description,''))
                  @@ plainto_tsquery('simple', ?)
              AND (?::text IS NULL OR d.document_type = ?)
              $tagFilter
        """.trimIndent()

        val countSql = "SELECT COUNT(*) FROM documents d WHERE $baseWhere"
        val countArgs = mutableListOf<Any?>(request.query, request.docType, request.docType)
        SearchSqlSupport.addTags(countArgs, tags)
        val totalElements = jdbcTemplate.queryForObject(countSql, Long::class.java, *countArgs.toTypedArray()) ?: 0L
        val totalPages = if (request.size > 0) ((totalElements + request.size - 1) / request.size).toInt() else 0

        val sql = """
            SELECT d.id, d.title, d.summary_short, d.document_type,
                   ${SearchSqlSupport.tagsProjection("d")},
                   ts_rank(to_tsvector('simple', coalesce(d.title,'') || ' ' || coalesce(d.description,'')),
                           plainto_tsquery('simple', ?)) AS score
            FROM documents d
            WHERE $baseWhere
            ORDER BY score DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val queryArgs = mutableListOf<Any?>(request.query, request.query, request.docType, request.docType)
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
