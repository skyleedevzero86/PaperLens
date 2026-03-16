package com.sleekydz86.paperlens.infrastructure.global.config

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!embedding")
class StubEmbeddingModelConfig {

    @Bean
    fun embeddingModel(): EmbeddingModel = object : EmbeddingModel {
        private fun fail(): Nothing = throw IllegalStateException(
            "Embedding is not enabled. Start with profile 'embedding' (e.g. --spring.profiles.active=embedding) and ensure " +
                "spring.ai.embedding.transformer.onnx.modelUri points to a valid ONNX file."
        )
        override fun call(request: EmbeddingRequest): EmbeddingResponse = fail()
        override fun embed(document: Document): FloatArray = fail()
    }
}
