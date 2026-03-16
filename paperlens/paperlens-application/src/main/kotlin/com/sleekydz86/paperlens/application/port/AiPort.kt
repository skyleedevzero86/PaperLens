package com.sleekydz86.paperlens.application.port

import com.sleekydz86.paperlens.application.dto.SummaryResult

interface AiPort {

    fun summarizeText(text: String): SummaryResult
    fun classifyDocumentType(text: String): String
    fun answerQuestion(question: String, context: String): String
}
