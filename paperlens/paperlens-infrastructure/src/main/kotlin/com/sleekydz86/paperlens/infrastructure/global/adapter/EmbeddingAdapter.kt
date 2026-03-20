package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.exception.EmbeddingNotAvailableException
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.infrastructure.global.cache.RedisCacheService
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.sqrt

@Component
class EmbeddingAdapter(
    @Lazy
    private val embeddingModel: EmbeddingModel,
    private val redisCacheService: RedisCacheService,
) : EmbeddingPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun embed(text: String): FloatArray {
        redisCacheService.getEmbedding(text)?.let { return it }
        val embedding = generateEmbedding(text)
        redisCacheService.putEmbedding(text, embedding)
        return embedding
    }

    override fun embedBatch(texts: List<String>): List<FloatArray> {
        val indexedTexts = texts.withIndex()
        val cached = mutableMapOf<Int, FloatArray>()
        val missingIndexes = mutableListOf<Int>()
        val missingTexts = mutableListOf<String>()

        indexedTexts.forEach { (index, text) ->
            val embedding = redisCacheService.getEmbedding(text)
            if (embedding != null) {
                cached[index] = embedding
            } else {
                missingIndexes.add(index)
                missingTexts.add(text)
            }
        }

        if (missingTexts.isNotEmpty()) {
            val generated = generateBatchEmbeddings(missingTexts)

            missingIndexes.zip(generated).forEach { (index, embedding) ->
                cached[index] = embedding
                redisCacheService.putEmbedding(texts[index], embedding)
            }
        }

        return texts.indices.map { index -> cached.getValue(index) }
    }

    private fun generateEmbedding(text: String): FloatArray =
        try {
            embeddingModel.embed(text)
        } catch (e: Exception) {
            logger.warn("Embedding model unavailable, using deterministic hash embedding fallback", e)
            hashEmbedding(text)
        }

    private fun generateBatchEmbeddings(texts: List<String>): List<FloatArray> =
        try {
            embeddingModel.embed(texts)
        } catch (e: Exception) {
            logger.warn("Embedding model unavailable for batch request, using deterministic hash embedding fallback", e)
            texts.map(::hashEmbedding)
        }

    private fun hashEmbedding(text: String): FloatArray {
        val vector = FloatArray(FALLBACK_DIMENSIONS)
        val normalizedText = text.lowercase()
        val tokens = TOKEN_REGEX.findAll(normalizedText)
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()

        if (tokens.isEmpty()) {
            vector[0] = 1f
            return vector
        }

        tokens.forEach { token ->
            val hash = sha256(token)
            repeat(4) { offset ->
                val indexSeed = toPositiveInt(hash, offset * 8)
                val signSeed = toPositiveInt(hash, offset * 8 + 4)
                val index = indexSeed % FALLBACK_DIMENSIONS
                val sign = if ((signSeed and 1) == 0) 1f else -1f
                vector[index] += sign
            }
        }

        val norm = sqrt(vector.fold(0.0) { acc, value -> acc + value * value }).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    private fun sha256(token: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))

    private fun toPositiveInt(bytes: ByteArray, start: Int): Int =
        ((bytes[start].toInt() and 0xff) shl 24) or
            ((bytes[start + 1].toInt() and 0xff) shl 16) or
            ((bytes[start + 2].toInt() and 0xff) shl 8) or
            (bytes[start + 3].toInt() and 0xff)

    private fun embeddingNotAvailable() =
        EmbeddingNotAvailableException(
            "Embedding model initialization failed. The cached ONNX file may be corrupted. " +
                "Delete %TEMP%\\spring-ai-onnx-generative or set " +
                "spring.ai.embedding.transformer.onnx.model-uri to a valid local ONNX file, then restart the server."
        )

    private companion object {
        private const val FALLBACK_DIMENSIONS = 384
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]{2,}")
    }
}
