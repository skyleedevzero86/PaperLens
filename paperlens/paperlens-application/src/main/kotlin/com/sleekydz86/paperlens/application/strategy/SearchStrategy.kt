package com.sleekydz86.paperlens.application.strategy

import com.sleekydz86.paperlens.application.dto.SearchRequest
import com.sleekydz86.paperlens.application.dto.SearchResponse
import com.sleekydz86.paperlens.domain.search.SearchMode

interface SearchStrategy {

    val mode: SearchMode
    fun search(request: SearchRequest): SearchResponse
}