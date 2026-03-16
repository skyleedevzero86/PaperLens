package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.FileStoragePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Component
class FileStorageAdapter(
    @Value("\${app.upload-dir:./uploads}") private val uploadDir: String,
) : FileStoragePort {

    private val basePath: Path by lazy {
        Files.createDirectories(Paths.get(uploadDir))
        Paths.get(uploadDir)
    }

    override fun save(fileBytes: ByteArray, fileName: String): String {
        val name = "${UUID.randomUUID()}_$fileName"
        val path = basePath.resolve(name)
        Files.write(path, fileBytes)
        return path.toString()
    }

    override fun read(path: String): ByteArray? {
        val p = Paths.get(path)
        return if (Files.exists(p)) Files.readAllBytes(p) else null
    }

    override fun delete(path: String) {
        Files.deleteIfExists(Paths.get(path))
    }
}
