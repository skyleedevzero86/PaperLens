package com.sleekydz86.paperlens.domain.shared

data class PageResult<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)