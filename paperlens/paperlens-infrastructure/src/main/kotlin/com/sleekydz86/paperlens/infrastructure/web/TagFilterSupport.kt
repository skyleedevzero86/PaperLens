package com.sleekydz86.paperlens.infrastructure.web

internal fun normalizeTagParams(vararg values: List<String>?): List<String> {
    val normalized = linkedSetOf<String>()
    values.asSequence()
        .filterNotNull()
        .flatten()
        .flatMap { it.split(',').asSequence() }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .forEach(normalized::add)
    return normalized.toList()
}
