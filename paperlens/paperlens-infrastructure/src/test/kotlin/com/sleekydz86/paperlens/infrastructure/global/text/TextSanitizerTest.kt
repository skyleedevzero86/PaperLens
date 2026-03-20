package com.sleekydz86.paperlens.infrastructure.global.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextSanitizerTest {

    @Test
    fun `sanitize removes null bytes and unsafe control characters`() {
        val raw = "A\u0000B\u0007C\nD\tE\rF"

        val sanitized = TextSanitizer.sanitize(raw)

        assertEquals("ABC\nD\tE\rF", sanitized)
    }
}