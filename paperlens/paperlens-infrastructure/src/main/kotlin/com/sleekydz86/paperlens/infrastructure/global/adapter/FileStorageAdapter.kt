package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.FileStoragePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedHashSet
import java.util.UUID

@Component
class FileStorageAdapter(
    @Value("\${app.upload-dir:./uploads}") private val uploadDir: String,
) : FileStoragePort {

    private val basePath: Path by lazy {
        val path = Paths.get(uploadDir).toAbsolutePath().normalize()
        Files.createDirectories(path)
        path
    }

    override fun save(fileBytes: ByteArray, fileName: String): String {
        val name = "${UUID.randomUUID()}_$fileName"
        val path = basePath.resolve(name)
        Files.write(path, fileBytes)
        return path.toString()
    }

    override fun read(path: String): ByteArray? {
        val resolved = resolveExistingPath(path) ?: return null
        return Files.readAllBytes(resolved)
    }

    override fun delete(path: String) {
        resolvePathCandidates(path)
            .firstOrNull { Files.exists(it) }
            ?.let { Files.deleteIfExists(it) }
    }

    private fun resolveExistingPath(path: String): Path? =
        resolvePathCandidates(path).firstOrNull { Files.isRegularFile(it) }

    private fun resolvePathCandidates(path: String): List<Path> {
        val raw = Paths.get(path)
        val normalized = raw.normalize()
        if (normalized.isAbsolute) {
            return listOf(normalized)
        }

        val candidates = LinkedHashSet<Path>()
        val fileName = normalized.fileName

        candidates.add(normalized.toAbsolutePath().normalize())
        candidates.add(basePath.resolve(normalized).normalize())
        if (fileName != null) {
            candidates.add(basePath.resolve(fileName).normalize())
        }

        if (normalized.nameCount > 1 && normalized.getName(0).toString().equals(basePath.fileName.toString(), ignoreCase = true)) {
            candidates.add(basePath.resolve(normalized.subpath(1, normalized.nameCount)).normalize())
        }

        var current: Path? = basePath.parent
        while (current != null) {
            candidates.add(current.resolve(normalized).normalize())
            if (fileName != null) {
                candidates.add(current.resolve(basePath.fileName).resolve(fileName).normalize())
            }
            current = current.parent
        }

        return candidates.toList()
    }
}
