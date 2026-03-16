package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.PdfPort
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class PdfAdapter : PdfPort {

    override fun getPageCount(fileBytes: ByteArray): Int =
        try {
            PDDocument.load(ByteArrayInputStream(fileBytes)).use { it.numberOfPages }
        } catch (_: Exception) {
            0
        }
}