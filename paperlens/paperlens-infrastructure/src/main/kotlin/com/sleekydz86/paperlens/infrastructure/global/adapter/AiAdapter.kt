package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.dto.SummaryResult
import com.sleekydz86.paperlens.application.port.AiPort
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class AiAdapter(
    @Lazy
    private val chatClient: ChatClient,
    @Value("\${spring.ai.huggingface.chat.url:}")
    private val chatUrl: String,
) : AiPort {

    override fun summarizeText(text: String): SummaryResult {
        val normalized = normalizeText(text).take(MAX_SUMMARY_INPUT)
        if (normalized.isBlank()) {
            return SummaryResult(
                short = "요약할 수 있는 문서 내용이 없습니다.",
                long = "문서에서 텍스트를 추출하지 못했습니다.",
                keywords = emptyList(),
            )
        }

        val fallback = fallbackSummary(normalized)
        if (!isChatConfigured()) return fallback

        return runCatching {
            val shortSummary = chatClient.prompt()
                .user(buildShortSummaryPrompt(normalized))
                .call()
                .content()
                ?.trim()
                .orEmpty()

            val longSummary = chatClient.prompt()
                .user(buildLongSummaryPrompt(normalized))
                .call()
                .content()
                ?.trim()
                .orEmpty()

            SummaryResult(
                short = shortSummary.ifBlank { fallback.short },
                long = longSummary.ifBlank { fallback.long },
                keywords = fallback.keywords,
            )
        }.getOrElse { fallback }
    }

    override fun classifyDocumentType(text: String): String {
        val normalized = normalizeText(text).take(MAX_CLASSIFICATION_INPUT)
        if (normalized.isBlank()) return "기타"
        if (!isChatConfigured()) return fallbackDocumentType(normalized)

        return runCatching {
            chatClient.prompt()
                .user(buildDocumentTypePrompt(normalized))
                .call()
                .content()
                ?.trim()
                .orEmpty()
        }.getOrNull().takeUnless { it.isNullOrBlank() } ?: fallbackDocumentType(normalized)
    }

    override fun answerQuestion(question: String, context: String): String {
        val normalizedContext = normalizeText(context).take(MAX_CONTEXT_INPUT)
        if (normalizedContext.isBlank()) {
            return "문서에서 관련 내용을 찾지 못했습니다."
        }

        if (!isChatConfigured()) {
            return fallbackAnswer(normalizedContext)
        }

        return runCatching {
            chatClient.prompt()
                .user(buildQuestionAnswerPrompt(question, normalizedContext))
                .call()
                .content()
                ?.trim()
                .orEmpty()
        }.getOrNull().takeUnless { it.isNullOrBlank() } ?: fallbackAnswer(normalizedContext)
    }

    private fun isChatConfigured(): Boolean = chatUrl.isNotBlank()

    private fun fallbackSummary(text: String): SummaryResult {
        val units = splitSummaryUnits(text)
        val shortSummary = units.take(3).joinToString(" ").ifBlank {
            text.take(240)
        }
        val longSummary = units.take(8).joinToString("\n").ifBlank {
            text.take(1200)
        }

        return SummaryResult(
            short = shortSummary.ifBlank { "요약할 수 있는 문장 정보를 찾지 못했습니다." },
            long = longSummary.ifBlank { "문서 본문이 비어 있습니다." },
            keywords = extractKeywords(text),
        )
    }

    private fun fallbackDocumentType(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("contract", "agreement", "terms", "계약", "약정").any(lower::contains) -> "계약서"
            listOf("manual", "guide", "instruction", "사용법", "매뉴얼").any(lower::contains) -> "매뉴얼"
            listOf("proposal", "제안", "제안서").any(lower::contains) -> "제안서"
            listOf("report", "analysis", "보고", "분석").any(lower::contains) -> "보고서"
            listOf("policy", "규정", "지침", "정책").any(lower::contains) -> "정책문서"
            listOf("api", "schema", "sql", "architecture", "기술", "설계").any(lower::contains) -> "기술문서"
            else -> "기타"
        }
    }

    private fun fallbackAnswer(context: String): String =
        buildString {
            append("AI 채팅 모델이 아직 설정되지 않아 문서에서 찾은 핵심 내용을 먼저 보여드립니다.\n\n")
            append(context.take(700))
        }

    private fun normalizeText(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()

    private fun splitSummaryUnits(text: String): List<String> {
        val units = text.split(Regex("(?<=[.!?。！？])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 12 }
            .distinct()

        if (units.isNotEmpty()) return units

        return text.chunked(160)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun extractKeywords(text: String): List<String> =
        KEYWORD_REGEX.findAll(text.lowercase())
            .map { it.value }
            .filter { token -> token.length >= 2 && token !in STOP_WORDS }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(5)
            .map { it.key }

    private fun buildShortSummaryPrompt(text: String): String = """
        You are summarizing a document for a Korean user.
        Read the document and return exactly 3 concise Korean sentences.
        Focus on the document's purpose, core points, and outcome.

        Document:
        $text
    """.trimIndent()

    private fun buildLongSummaryPrompt(text: String): String = """
        You are summarizing a document for a Korean user.
        Write a structured Korean summary with short sections.
        Include the purpose, main points, and any notable decisions or actions.
        Keep it faithful to the source and do not invent facts.

        Document:
        $text
    """.trimIndent()

    private fun buildDocumentTypePrompt(text: String): String = """
        Classify the following document into one Korean label only.
        Available labels: 계약서, 매뉴얼, 제안서, 보고서, 정책문서, 기술문서, 기타

        Document:
        $text
    """.trimIndent()

    private fun buildQuestionAnswerPrompt(question: String, context: String): String = """
        Answer the user's question in Korean using only the document context below.
        If the answer is not supported by the context, say that the document does not contain enough information.

        Context:
        $context

        Question:
        $question
    """.trimIndent()

    private companion object {
        private const val MAX_SUMMARY_INPUT = 7000
        private const val MAX_CLASSIFICATION_INPUT = 2500
        private const val MAX_CONTEXT_INPUT = 8000
        private val KEYWORD_REGEX = Regex("[\\p{L}\\p{N}]{2,}")
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "that", "this", "from", "into", "have",
            "문서", "내용", "대한", "에서", "합니다", "있는", "하는", "입니다", "그리고", "또한"
        )
    }
}
