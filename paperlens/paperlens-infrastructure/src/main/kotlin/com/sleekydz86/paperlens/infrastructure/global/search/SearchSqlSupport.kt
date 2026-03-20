package com.sleekydz86.paperlens.infrastructure.global.search

object SearchSqlSupport {
    private const val TAG_DELIMITER = "|||"

    fun normalizeTags(tags: List<String>): List<String> =
        tags.map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

    fun buildTagFilter(docAlias: String, tags: List<String>): String {
        if (tags.isEmpty()) return ""
        val placeholders = List(tags.size) { "?" }.joinToString(", ")
        return """
            AND EXISTS (
                SELECT 1
                FROM document_tags dt
                WHERE dt.document_id = $docAlias.id
                  AND dt.tag_name IN ($placeholders)
            )
        """.trimIndent()
    }

    fun addTags(target: MutableList<Any?>, tags: List<String>) {
        target.addAll(tags)
    }

    fun tagsProjection(docAlias: String): String = """
        COALESCE((
            SELECT string_agg(DISTINCT dt.tag_name, '$TAG_DELIMITER' ORDER BY dt.tag_name)
            FROM document_tags dt
            WHERE dt.document_id = $docAlias.id
        ), '') AS tags
    """.trimIndent()

    fun parseTags(raw: String?): List<String> =
        raw?.split(TAG_DELIMITER)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?: emptyList()
}
