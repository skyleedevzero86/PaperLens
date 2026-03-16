package com.sleekydz86.paperlens.application.port

interface EmbeddingPort {

    fun embed(text: String): FloatArray
    fun embedBatch(texts: List<String>): List<FloatArray>
}