package com.sleekydz86.paperlens.infrastructure.persistence.entity

import com.sleekydz86.paperlens.domain.document.DocumentStatus
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
@SQLRestriction("deleted_at IS NULL")
class DocumentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    val originalFileName: String,

    @Column(nullable = false)
    val storagePath: String,

    val mimeType: String = "application/pdf",
    var pageCount: Int = 0,
    var fileSize: Long = 0,
    var documentType: String? = null,

    @Column(columnDefinition = "TEXT")
    var summaryShort: String? = null,

    @Column(columnDefinition = "TEXT")
    var summaryLong: String? = null,

    @Enumerated(EnumType.STRING)
    var status: DocumentStatus = DocumentStatus.PENDING,

    val createdBy: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tags: MutableList<DocumentTagEntity> = mutableListOf(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val chunks: MutableList<DocumentChunkEntity> = mutableListOf(),
) {
    internal constructor() : this(
        id = 0,
        title = "",
        description = null,
        originalFileName = "",
        storagePath = "",
        mimeType = "application/pdf",
        pageCount = 0,
        fileSize = 0,
        documentType = null,
        summaryShort = null,
        summaryLong = null,
        status = DocumentStatus.PENDING,
        createdBy = 0L,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        deletedAt = null,
        tags = mutableListOf(),
        chunks = mutableListOf(),
    )
}
