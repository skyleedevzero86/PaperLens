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
        val truncated = text.take(6000)
        if (!isChatConfigured()) {
            return fallbackSummary(truncated)
        }

        return runCatching {
            val shortPrompt = """
                다음 문서 내용을 3문장 이내로 간단히 요약해주세요.
                ---
                $truncated
                ---
                요약:
            """.trimIndent()
            val shortSummary = chatClient.prompt().user(shortPrompt).call().content() ?: fallbackSummary(truncated).short

            val longPrompt = """
                다음 문서 내용을 자세히 요약해주세요. 주요 섹션별로 정리해주세요.
                ---
                $truncated
                ---
                상세 요약:
            """.trimIndent()
            val longSummary = chatClient.prompt().user(longPrompt).call().content() ?: fallbackSummary(truncated).long

            SummaryResult(short = shortSummary, long = longSummary, keywords = emptyList())
        }.getOrElse {
            fallbackSummary(truncated)
        }
    }

    override fun classifyDocumentType(text: String): String {
        val truncated = text.take(2000)
        if (!isChatConfigured()) {
            return fallbackDocumentType(truncated)
        }

        return runCatching {
            val prompt = """
                다음 문서의 유형을 한 단어로 분류해주세요.
                분류 옵션: 계약서, 매뉴얼, 제안서, 보고서, 정책문서, 기술문서, 기타
                ---
                $truncated
                ---
                문서유형:
            """.trimIndent()
            chatClient.prompt().user(prompt).call().content()?.trim()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallbackDocumentType(truncated)
    }

    override fun answerQuestion(question: String, context: String): String {
        if (context.isBlank()) {
            return "문서에서 관련 내용을 찾지 못했습니다."
        }
        if (!isChatConfigured()) {
            return fallbackAnswer(context)
        }

        return runCatching {
            val prompt = """
                다음 문서 내용을 바탕으로 질문에 답해주세요.
                문서에 없는 내용은 추측하지 말고 '문서에서 찾을 수 없습니다'라고 답해주세요.

                [문서 내용]
                $context

                [질문]
                $question

                [답변]
            """.trimIndent()
            chatClient.prompt().user(prompt).call().content()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallbackAnswer(context)
    }

    private fun isChatConfigured(): Boolean = chatUrl.isNotBlank()

    private fun fallbackSummary(text: String): SummaryResult {
        val units = text.split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val shortSummary = units.take(3).joinToString(" ").ifBlank {
            text.take(240).ifBlank { "요약할 내용이 없습니다." }
        }
        val longSummary = units.take(8).joinToString("\n").ifBlank {
            text.take(1200).ifBlank { "요약할 내용이 없습니다." }
        }

        return SummaryResult(short = shortSummary, long = longSummary, keywords = emptyList())
    }

    private fun fallbackDocumentType(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("계약", "agreement", "terms").any { lower.contains(it) } -> "계약서"
            listOf("매뉴얼", "manual", "사용법").any { lower.contains(it) } -> "매뉴얼"
            listOf("제안", "proposal", "제안서").any { lower.contains(it) } -> "제안서"
            listOf("보고", "report", "분석").any { lower.contains(it) } -> "보고서"
            listOf("정책", "policy", "규정").any { lower.contains(it) } -> "정책문서"
            listOf("기술", "sql", "api", "설계", "아키텍처").any { lower.contains(it) } -> "기술문서"
            else -> "기타"
        }
    }

    private fun fallbackAnswer(context: String): String =
        buildString {
            append("채팅 모델이 설정되지 않아 문서에서 찾은 관련 내용을 그대로 보여드립니다.\n\n")
            append(context.take(700))
        }
}
