package com.sleekydz86.paperlens.infrastructure.global.text

object TextSanitizer {

    fun sanitize(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            when {
                ch == '\u0000' -> Unit
                ch.isISOControl() && ch != '\n' && ch != '\r' && ch != '\t' -> Unit
                else -> append(ch)
            }
        }
    }
}