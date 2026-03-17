package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.exception.EmbeddingNotAvailableException
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class EmbeddingAdapter(
    @Lazy
    private val embeddingModel: EmbeddingModel,
) : EmbeddingPort {

    override fun embed(text: String): FloatArray =
        try {
            embeddingModel.embed(text)
        } catch (_: Exception) {
            throw EmbeddingNotAvailableException(
                "Embedding model initialization failed. The cached ONNX file may be corrupted. " +
                    "Delete %TEMP%\\spring-ai-onnx-generative or set " +
                    "spring.ai.embedding.transformer.onnx.model-uri to a valid local ONNX file, then restart the server."
            )
        }

    override fun embedBatch(texts: List<String>): List<FloatArray> =
        try {
            embeddingModel.embed(texts)
        } catch (_: Exception) {
            throw EmbeddingNotAvailableException(
                "Embedding model initialization failed. The cached ONNX file may be corrupted. " +
                    "Delete %TEMP%\\spring-ai-onnx-generative or set " +
                    "spring.ai.embedding.transformer.onnx.model-uri to a valid local ONNX file, then restart the server."
            )
        }
}
