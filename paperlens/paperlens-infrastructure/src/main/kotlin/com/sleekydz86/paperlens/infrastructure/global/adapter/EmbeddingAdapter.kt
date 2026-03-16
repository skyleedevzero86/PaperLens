package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.EmbeddingPort
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Component

@Component
class EmbeddingAdapter(
    private val embeddingModel: EmbeddingModel,
) : EmbeddingPort {

    override fun embed(text: String): FloatArray = embeddingModel.embed(text)

    override fun embedBatch(texts: List<String>): List<FloatArray> = embeddingModel.embed(texts)
}
