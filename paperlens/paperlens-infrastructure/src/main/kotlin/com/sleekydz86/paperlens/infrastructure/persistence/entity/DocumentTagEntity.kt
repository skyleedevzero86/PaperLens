package com.sleekydz86.paperlens.infrastructure.persistence.entity

import jakarta.persistence.*

@Entity
@Table(name = "document_tags")
class DocumentTagEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: DocumentEntity,

    @Column(nullable = false)
    val tagName: String,
) {
    internal constructor() : this(document = DocumentEntity(), tagName = "")
}
