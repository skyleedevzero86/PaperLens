package com.sleekydz86.paperlens.infrastructure.persistence.entity

import com.sleekydz86.paperlens.domain.job.DocumentJobStatus
import com.sleekydz86.paperlens.domain.job.DocumentJobType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "document_jobs")
class DocumentJobEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: DocumentEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var jobType: DocumentJobType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DocumentJobStatus = DocumentJobStatus.RUNNING,

    @Column(nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),

    var finishedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,
) {
    internal constructor() : this(
        document = DocumentEntity(),
        jobType = DocumentJobType.PARSE,
    )
}
