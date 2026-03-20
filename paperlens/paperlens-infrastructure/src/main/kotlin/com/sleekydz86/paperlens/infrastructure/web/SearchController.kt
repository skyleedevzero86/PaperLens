package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.usecase.SearchUseCase
import com.sleekydz86.paperlens.domain.search.SearchMode
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchUseCase: SearchUseCase,
    private val redisCacheService: RedisCacheService,
) {

    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "HYBRID") mode: SearchMode,
        @RequestParam(required = false) docType: String?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(name = "tag", required = false) tag: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Any> {
        val mergedTags = normalizeTagParams(tags, tag)
        val request = SearchRequest(query, mode, docType, mergedTags, page, size)
        redisCacheService.getSearch(request)?.let { return ResponseEntity.ok(it) }

        val response = searchUseCase.search(request)
        redisCacheService.putSearch(request, response)
        return ResponseEntity.ok(response)
    }
}
