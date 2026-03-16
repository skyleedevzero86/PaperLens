package com.sleekydz86.paperlens.infrastructure.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "document_chunks")
class DocumentChunkEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: DocumentEntity,

    val pageFrom: Int,
    val pageTo: Int,
    val chunkIndex: Int,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    val tokenCount: Int = 0,

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(384)")
    var embedding: FloatArray? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    internal constructor() : this(
        document = DocumentEntity(),
        pageFrom = 0,
        pageTo = 0,
        chunkIndex = 0,
        content = "",
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DocumentChunkEntity
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id.hashCode()
}