package com.sleekydz86.paperlens.application.usecase

import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.dto.SearchResponse
import com.sleekydz86.paperlens.application.strategy.SearchStrategy
import com.sleekydz86.paperlens.domain.search.SearchMode

class SearchUseCase(
    strategies: List<SearchStrategy>,
) {
    private val strategyMap: Map<SearchMode, SearchStrategy> = strategies.associateBy { it.mode }

    fun search(request: SearchRequest): SearchResponse {
        val strategy = strategyMap[request.mode]
            ?: throw IllegalArgumentException("지원하지 않는 검색 모드입니다: ${request.mode}")
        return strategy.search(request)
    }
}