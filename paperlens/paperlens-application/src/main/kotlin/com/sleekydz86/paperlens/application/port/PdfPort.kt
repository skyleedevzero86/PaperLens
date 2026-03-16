package com.sleekydz86.paperlens.application.port

interface PdfPort {

    fun getPageCount(fileBytes: ByteArray): Int
}