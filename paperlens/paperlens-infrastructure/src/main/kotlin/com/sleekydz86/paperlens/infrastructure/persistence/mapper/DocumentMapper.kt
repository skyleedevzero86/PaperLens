package com.sleekydz86.paperlens.infrastructure.persistence.mapper

import com.sleekydz86.paperlens.domain.document.Document
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentEntity
import com.sleekydz86.paperlens.infrastructure.persistence.entity.DocumentTagEntity
import org.hibernate.Hibernate
import java.time.LocalDateTime

object DocumentMapper {

    fun toDomain(e: DocumentEntity): Document = Document(
        id = e.id,
        title = e.title,
        description = e.description,
        originalFileName = e.originalFileName,
        storagePath = e.storagePath,
        mimeType = e.mimeType,
        pageCount = e.pageCount,
        fileSize = e.fileSize,
        documentType = e.documentType,
        summaryShort = e.summaryShort,
        summaryLong = e.summaryLong,
        status = e.status,
        createdBy = e.createdBy,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        deletedAt = e.deletedAt,
        tagNames = if (Hibernate.isInitialized(e.tags)) e.tags.map { it.tagName } else emptyList(),
    )

    fun toEntity(d: Document): DocumentEntity {
        val entity = DocumentEntity(
            id = d.id,
            title = d.title,
            description = d.description,
            originalFileName = d.originalFileName,
            storagePath = d.storagePath,
            mimeType = d.mimeType,
            pageCount = d.pageCount,
            fileSize = d.fileSize,
            documentType = d.documentType,
            summaryShort = d.summaryShort,
            summaryLong = d.summaryLong,
            status = d.status,
            createdBy = d.createdBy,
            createdAt = d.createdAt,
            updatedAt = d.updatedAt,
            deletedAt = d.deletedAt,
        )
        d.tagNames.forEach { entity.tags.add(DocumentTagEntity(document = entity, tagName = it)) }
        return entity
    }

    fun updateEntity(entity: DocumentEntity, d: Document): DocumentEntity {
        entity.title = d.title
        entity.description = d.description
        entity.summaryShort = d.summaryShort
        entity.summaryLong = d.summaryLong
        entity.documentType = d.documentType
        entity.status = d.status
        entity.updatedAt = d.updatedAt
        entity.deletedAt = d.deletedAt
        entity.pageCount = d.pageCount
        entity.fileSize = d.fileSize
        entity.tags.clear()
        d.tagNames.forEach { entity.tags.add(DocumentTagEntity(document = entity, tagName = it)) }
        return entity
    }
}
