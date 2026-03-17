package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.PdfPort
import org.apache.pdfbox.Loader
import org.springframework.stereotype.Component

@Component
class PdfAdapter : PdfPort {

    override fun getPageCount(fileBytes: ByteArray): Int =
        try {
            Loader.loadPDF(fileBytes).use { it.numberOfPages }
        } catch (_: Exception) {
            0
        }
}
