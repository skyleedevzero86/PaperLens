package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.dto.SummaryResult
import com.sleekydz86.paperlens.application.port.AiPort
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

@Component
class AiAdapter(
    private val chatClient: ChatClient,
) : AiPort {

    override fun summarizeText(text: String): SummaryResult {
        val truncated = text.take(6000)
        val shortPrompt = """
            다음 문서 내용을 3문장 이내로 핵심만 요약해주세요.
            ---
            $truncated
            ---
            요약:
        """.trimIndent()
        val shortSummary = chatClient.prompt().user(shortPrompt).call().content() ?: "요약을 생성할 수 없습니다."

        val longPrompt = """
            다음 문서 내용을 상세하게 요약해주세요. 주요 섹션별로 정리하고 핵심 내용을 포함해주세요.
            ---
            $truncated
            ---
            상세 요약:
        """.trimIndent()
        val longSummary = chatClient.prompt().user(longPrompt).call().content() ?: "요약을 생성할 수 없습니다."

        return SummaryResult(short = shortSummary, long = longSummary, keywords = emptyList())
    }

    override fun classifyDocumentType(text: String): String {
        val prompt = """
            다음 문서의 유형을 한 단어로 분류해주세요.
            분류 옵션: 계약서, 매뉴얼, 제안서, 보고서, 정책문서, 기술문서, 기타
            ---
            ${text.take(2000)}
            ---
            문서유형:
        """.trimIndent()
        return chatClient.prompt().user(prompt).call().content()?.trim() ?: "기타"
    }

    override fun answerQuestion(question: String, context: String): String {
        val prompt = """
            다음 문서 내용을 바탕으로 질문에 답변해주세요.
            문서 내용에 없는 정보는 추측하지 말고 "문서에서 찾을 수 없습니다"라고 답해주세요.

            [문서 내용]
            $context

            [질문]
            $question

            [답변]
        """.trimIndent()
        return chatClient.prompt().user(prompt).call().content() ?: "답변을 생성할 수 없습니다."
    }
}
