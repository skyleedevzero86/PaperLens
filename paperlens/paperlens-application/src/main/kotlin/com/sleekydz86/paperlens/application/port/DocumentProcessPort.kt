package com.sleekydz86.paperlens.application.port

interface DocumentProcessPort {

    fun processAsync(documentId: Long)
}