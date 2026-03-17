package com.sleekydz86.paperlens.infrastructure.global.config

import com.sleekydz86.paperlens.application.exception.EmbeddingNotAvailableException
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("no-embedding")
class StubEmbeddingModelConfig {

    @Bean
    fun embeddingModel(): EmbeddingModel = object : EmbeddingModel {
        private fun fail(): Nothing = throw EmbeddingNotAvailableException(
            "임베딩이 비활성화되어 있습니다. 현재 'no-embedding' 프로필로 기동 중입니다. " +
                "'no-embedding' 프로필을 제거하고 기동하거나 " +
                "spring.ai.embedding.transformer.onnx.model-uri 와 " +
                "spring.ai.embedding.transformer.tokenizer.uri 에 유효한 경로를 설정하세요."
        )

        override fun call(request: EmbeddingRequest): EmbeddingResponse = fail()

        override fun embed(document: Document): FloatArray = fail()
    }
}
