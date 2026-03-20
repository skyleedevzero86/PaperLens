package com.sleekydz86.paperlens.infrastructure.global.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.paperlens.application.dto.AdminStatsResponse
import com.sleekydz86.paperlens.application.dto.CacheMetricResponse
import com.sleekydz86.paperlens.application.dto.CacheStatsResponse
import com.sleekydz86.paperlens.application.dto.DocumentDetailResponse
import com.sleekydz86.paperlens.application.dto.DocumentJobResponse
import com.sleekydz86.paperlens.application.dto.DocumentResponse
import com.sleekydz86.paperlens.application.dto.PageResponse
import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.dto.SearchResponse
import com.sleekydz86.paperlens.application.dto.SimilarDocumentResult
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class RedisCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.cache.redis-enabled:true}")
    private val enabled: Boolean,
    @Value("\${app.cache.search-ttl:10m}")
    private val searchTtl: Duration,
    @Value("\${app.cache.document-list-ttl:5m}")
    private val documentListTtl: Duration,
    @Value("\${app.cache.document-detail-ttl:10m}")
    private val documentDetailTtl: Duration,
    @Value("\${app.cache.tags-ttl:30m}")
    private val tagsTtl: Duration,
    @Value("\${app.cache.embedding-ttl:24h}")
    private val embeddingTtl: Duration,
    @Value("\${app.cache.similar-documents-ttl:10m}")
    private val similarDocumentsTtl: Duration,
    @Value("\${app.cache.admin-ttl:1m}")
    private val adminTtl: Duration,
    @Value("\${app.cache.viewer-info-ttl:10m}")
    private val viewerInfoTtl: Duration,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val metrics = ConcurrentHashMap<String, CacheCounters>()

    fun getSearch(request: SearchRequest): SearchResponse? =
        readJson(searchKey(request), CACHE_SEARCH, object : TypeReference<SearchResponse>() {})

    fun putSearch(request: SearchRequest, response: SearchResponse) {
        writeJson(searchKey(request), CACHE_SEARCH, response, searchTtl)
    }

    fun getDocumentList(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int): PageResponse<DocumentResponse>? =
        readJson(documentListKey(keyword, docType, tags, page, size), CACHE_DOCUMENT_LIST, object : TypeReference<PageResponse<DocumentResponse>>() {})

    fun putDocumentList(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int, response: PageResponse<DocumentResponse>) {
        writeJson(documentListKey(keyword, docType, tags, page, size), CACHE_DOCUMENT_LIST, response, documentListTtl)
    }

    fun getDocumentDetail(documentId: Long): DocumentDetailResponse? =
        readJson(documentDetailKey(documentId), CACHE_DOCUMENT_DETAIL, object : TypeReference<DocumentDetailResponse>() {})

    fun putDocumentDetail(documentId: Long, response: DocumentDetailResponse) {
        writeJson(documentDetailKey(documentId), CACHE_DOCUMENT_DETAIL, response, documentDetailTtl)
    }

    fun getAvailableTags(): List<String>? =
        readJson(tagsKey(), CACHE_TAGS, object : TypeReference<List<String>>() {})

    fun putAvailableTags(tags: List<String>) {
        writeJson(tagsKey(), CACHE_TAGS, tags, tagsTtl)
    }

    fun getEmbedding(text: String): FloatArray? =
        readJson(embeddingKey(text), CACHE_EMBEDDING, object : TypeReference<List<Float>>() {})
            ?.let { values -> FloatArray(values.size) { index -> values[index] } }

    fun putEmbedding(text: String, embedding: FloatArray) {
        writeJson(embeddingKey(text), CACHE_EMBEDDING, embedding.toList(), embeddingTtl)
    }

    fun getSimilarDocuments(documentId: Long, limit: Int): List<SimilarDocumentResult>? =
        readJson(similarDocumentsKey(documentId, limit), CACHE_SIMILAR_DOCUMENTS, object : TypeReference<List<SimilarDocumentResult>>() {})

    fun putSimilarDocuments(documentId: Long, limit: Int, response: List<SimilarDocumentResult>) {
        writeJson(similarDocumentsKey(documentId, limit), CACHE_SIMILAR_DOCUMENTS, response, similarDocumentsTtl)
    }

    fun getAdminStats(): AdminStatsResponse? =
        readJson(adminStatsKey(), CACHE_ADMIN_STATS, object : TypeReference<AdminStatsResponse>() {})

    fun putAdminStats(response: AdminStatsResponse) {
        writeJson(adminStatsKey(), CACHE_ADMIN_STATS, response, adminTtl)
    }

    fun getFailedDocuments(): List<DocumentResponse>? =
        readJson(adminFailedDocumentsKey(), CACHE_ADMIN_FAILED_DOCUMENTS, object : TypeReference<List<DocumentResponse>>() {})

    fun putFailedDocuments(response: List<DocumentResponse>) {
        writeJson(adminFailedDocumentsKey(), CACHE_ADMIN_FAILED_DOCUMENTS, response, adminTtl)
    }

    fun getRecentDocumentJobs(limit: Int): List<DocumentJobResponse>? =
        readJson(adminRecentJobsKey(limit), CACHE_ADMIN_DOCUMENT_JOBS, object : TypeReference<List<DocumentJobResponse>>() {})

    fun putRecentDocumentJobs(limit: Int, response: List<DocumentJobResponse>) {
        writeJson(adminRecentJobsKey(limit), CACHE_ADMIN_DOCUMENT_JOBS, response, adminTtl)
    }

    fun getDocumentJobs(documentId: Long): List<DocumentJobResponse>? =
        readJson(documentJobsKey(documentId), CACHE_DOCUMENT_JOB_HISTORY, object : TypeReference<List<DocumentJobResponse>>() {})

    fun putDocumentJobs(documentId: Long, response: List<DocumentJobResponse>) {
        writeJson(documentJobsKey(documentId), CACHE_DOCUMENT_JOB_HISTORY, response, adminTtl)
    }

    fun getViewerInfo(documentId: Long): Map<String, Any>? =
        readJson(viewerInfoKey(documentId), CACHE_VIEWER_INFO, object : TypeReference<Map<String, Any>>() {})

    fun putViewerInfo(documentId: Long, response: Map<String, Any>) {
        writeJson(viewerInfoKey(documentId), CACHE_VIEWER_INFO, response, viewerInfoTtl)
    }

    fun getStats(): CacheStatsResponse =
        CacheStatsResponse(
            enabled = enabled,
            metrics = metrics.entries
                .sortedBy { it.key }
                .map { (cacheName, counters) ->
                    val hits = counters.hits.get()
                    val misses = counters.misses.get()
                    val totalReads = hits + misses
                    CacheMetricResponse(
                        cacheName = cacheName,
                        hits = hits,
                        misses = misses,
                        puts = counters.puts.get(),
                        evictions = counters.evictions.get(),
                        errors = counters.errors.get(),
                        hitRate = if (totalReads == 0L) 0.0 else hits.toDouble() / totalReads.toDouble(),
                    )
                },
        )

    fun evictDocumentCaches() {
        deleteByPattern("$CACHE_PREFIX:search:*", CACHE_SEARCH)
        deleteByPattern("$CACHE_PREFIX:documents:list:*", CACHE_DOCUMENT_LIST)
        deleteByPattern("$CACHE_PREFIX:documents:detail:*", CACHE_DOCUMENT_DETAIL)
        deleteByPattern("$CACHE_PREFIX:tags:*", CACHE_TAGS)
        deleteByPattern("$CACHE_PREFIX:ai:similar:*", CACHE_SIMILAR_DOCUMENTS)
        deleteByPattern("$CACHE_PREFIX:viewer:info:*", CACHE_VIEWER_INFO)
        deleteByPattern("$CACHE_PREFIX:admin:stats:*", CACHE_ADMIN_STATS)
        deleteByPattern("$CACHE_PREFIX:admin:failed-documents:*", CACHE_ADMIN_FAILED_DOCUMENTS)
        deleteByPattern("$CACHE_PREFIX:admin:document-jobs:*", CACHE_ADMIN_DOCUMENT_JOBS)
        deleteByPattern("$CACHE_PREFIX:admin:document-history:*", CACHE_DOCUMENT_JOB_HISTORY)
    }

    private fun tagsKey(): String = "$CACHE_PREFIX:tags:all"

    private fun searchKey(request: SearchRequest): String {
        val signature = listOf(
            request.mode.name,
            request.query.trim(),
            request.docType?.trim().orEmpty(),
            normalizeTags(request.tags).joinToString(","),
            request.page.toString(),
            request.size.toString(),
        ).joinToString("|")
        return "$CACHE_PREFIX:search:${sha256(signature)}"
    }

    private fun documentListKey(keyword: String?, docType: String?, tags: List<String>, page: Int, size: Int): String {
        val signature = listOf(
            keyword?.trim().orEmpty(),
            docType?.trim().orEmpty(),
            normalizeTags(tags).joinToString(","),
            page.toString(),
            size.toString(),
        ).joinToString("|")
        return "$CACHE_PREFIX:documents:list:${sha256(signature)}"
    }

    private fun documentDetailKey(documentId: Long): String =
        "$CACHE_PREFIX:documents:detail:$documentId"

    private fun embeddingKey(text: String): String =
        "$CACHE_PREFIX:embedding:${sha256(text.trim())}"

    private fun similarDocumentsKey(documentId: Long, limit: Int): String =
        "$CACHE_PREFIX:ai:similar:$documentId:$limit"

    private fun adminStatsKey(): String = "$CACHE_PREFIX:admin:stats:all"

    private fun adminFailedDocumentsKey(): String = "$CACHE_PREFIX:admin:failed-documents:all"

    private fun adminRecentJobsKey(limit: Int): String =
        "$CACHE_PREFIX:admin:document-jobs:$limit"

    private fun documentJobsKey(documentId: Long): String =
        "$CACHE_PREFIX:admin:document-history:$documentId"

    private fun viewerInfoKey(documentId: Long): String =
        "$CACHE_PREFIX:viewer:info:$documentId"

    private fun normalizeTags(tags: List<String>): List<String> =
        tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun deleteByPattern(pattern: String, cacheName: String) {
        if (!enabled) return
        runCatching {
            val keys = redisTemplate.keys(pattern)
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
                record(cacheName, Operation.EVICT, keys.size.toLong())
            }
        }.onFailure {
            record(cacheName, Operation.ERROR)
            logger.warn("Failed to evict Redis keys by pattern: {}", pattern, it)
        }
    }

    private fun writeJson(key: String, cacheName: String, value: Any, ttl: Duration) {
        if (!enabled) return
        runCatching {
            val json = objectMapper.writeValueAsString(value)
            redisTemplate.opsForValue().set(key, json, ttl)
            record(cacheName, Operation.PUT)
        }.onFailure {
            record(cacheName, Operation.ERROR)
            logger.warn("Failed to write Redis cache key: {}", key, it)
        }
    }

    private fun <T> readJson(key: String, cacheName: String, typeReference: TypeReference<T>): T? {
        if (!enabled) return null
        return runCatching {
            val json = redisTemplate.opsForValue().get(key)
            if (json == null) {
                record(cacheName, Operation.MISS)
                null
            } else {
                record(cacheName, Operation.HIT)
                objectMapper.readValue(json, typeReference)
            }
        }.onFailure {
            record(cacheName, Operation.ERROR)
            logger.warn("Failed to read Redis cache key: {}", key, it)
        }.getOrNull()
    }

    private fun record(cacheName: String, operation: Operation, amount: Long = 1L) {
        val counters = metrics.computeIfAbsent(cacheName) { CacheCounters() }
        when (operation) {
            Operation.HIT -> counters.hits.addAndGet(amount)
            Operation.MISS -> counters.misses.addAndGet(amount)
            Operation.PUT -> counters.puts.addAndGet(amount)
            Operation.EVICT -> counters.evictions.addAndGet(amount)
            Operation.ERROR -> counters.errors.addAndGet(amount)
        }
        meterRegistry.counter(
            "paperlens.cache.operations",
            "cache", cacheName,
            "operation", operation.name.lowercase(),
        ).increment(amount.toDouble())
    }

    private class CacheCounters(
        val hits: AtomicLong = AtomicLong(0),
        val misses: AtomicLong = AtomicLong(0),
        val puts: AtomicLong = AtomicLong(0),
        val evictions: AtomicLong = AtomicLong(0),
        val errors: AtomicLong = AtomicLong(0),
    )

    private enum class Operation {
        HIT,
        MISS,
        PUT,
        EVICT,
        ERROR,
    }

    companion object {
        private const val CACHE_PREFIX = "paperlens:cache"
        private const val CACHE_SEARCH = "search"
        private const val CACHE_DOCUMENT_LIST = "document-list"
        private const val CACHE_DOCUMENT_DETAIL = "document-detail"
        private const val CACHE_TAGS = "tags"
        private const val CACHE_EMBEDDING = "embedding"
        private const val CACHE_SIMILAR_DOCUMENTS = "similar-documents"
        private const val CACHE_ADMIN_STATS = "admin-stats"
        private const val CACHE_ADMIN_FAILED_DOCUMENTS = "admin-failed-documents"
        private const val CACHE_ADMIN_DOCUMENT_JOBS = "admin-document-jobs"
        private const val CACHE_DOCUMENT_JOB_HISTORY = "document-job-history"
        private const val CACHE_VIEWER_INFO = "viewer-info"
    }
}
