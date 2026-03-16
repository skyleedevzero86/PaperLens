package com.sleekydz86.paperlens.application.port

interface FileStoragePort {

    fun save(fileBytes: ByteArray, fileName: String): String
    fun read(path: String): ByteArray?
    fun delete(path: String)
}