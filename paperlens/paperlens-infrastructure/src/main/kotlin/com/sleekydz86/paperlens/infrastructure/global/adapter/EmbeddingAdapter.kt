package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.EmbeddingPort
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Component

@Component
class EmbeddingAdapter(
    private val embeddingModel: EmbeddingModel,
) : EmbeddingPort {

    override fun embed(text: String): FloatArray {
        val response = embeddingModel.embedForResponse(listOf(text))
        return response.results[0].output.toFloatArray()
    }

    override fun embedBatch(texts: List<String>): List<FloatArray> {
        val response = embeddingModel.embedForResponse(texts)
        return response.results.map { it.output.toFloatArray() }
    }

    private fun List<Double>.toFloatArray(): FloatArray = FloatArray(size) { this[it].toFloat() }
}
