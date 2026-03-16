package com.sleekydz86.paperlens.domain.document

import java.time.LocalDateTime

data class DocumentChunk(
    val id: Long,
    val documentId: Long,
    val pageFrom: Int,
    val pageTo: Int,
    val chunkIndex: Int,
    val content: String,
    val tokenCount: Int,
    val embedding: FloatArray?,
    val createdAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DocumentChunk
        if (id != other.id) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}