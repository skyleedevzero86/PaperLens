package com.sleekydz86.paperlens.infrastructure.global.config

import com.sleekydz86.paperlens.application.port.AiPort
import com.sleekydz86.paperlens.application.port.AuthPort
import com.sleekydz86.paperlens.application.port.DocumentJobPort
import com.sleekydz86.paperlens.application.port.DocumentProcessPort
import com.sleekydz86.paperlens.application.port.EmbeddingPort
import com.sleekydz86.paperlens.application.port.FileStoragePort
import com.sleekydz86.paperlens.application.port.PasswordEncoderPort
import com.sleekydz86.paperlens.application.port.PdfPort
import com.sleekydz86.paperlens.application.port.QueryLogPort
import com.sleekydz86.paperlens.application.port.TokenPort
import com.sleekydz86.paperlens.application.port.VectorSearchPort
import com.sleekydz86.paperlens.application.strategy.SearchStrategy
import com.sleekydz86.paperlens.application.usecase.AdminUseCase
import com.sleekydz86.paperlens.application.usecase.AiUseCase
import com.sleekydz86.paperlens.application.usecase.AuthUseCase
import com.sleekydz86.paperlens.application.usecase.DocumentUseCase
import com.sleekydz86.paperlens.application.usecase.SearchUseCase
import com.sleekydz86.paperlens.domain.port.DocumentChunkRepositoryPort
import com.sleekydz86.paperlens.domain.port.DocumentRepositoryPort
import com.sleekydz86.paperlens.domain.port.UserRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

    @Bean
    fun documentUseCase(
        documentRepository: DocumentRepositoryPort,
        chunkRepository: DocumentChunkRepositoryPort,
        fileStorage: FileStoragePort,
        processPort: DocumentProcessPort,
        pdfPort: PdfPort,
        documentJobPort: DocumentJobPort,
        aiPort: AiPort,
    ) = DocumentUseCase(documentRepository, chunkRepository, fileStorage, processPort, pdfPort, documentJobPort, aiPort)

    @Bean
    fun authUseCase(
        userRepository: UserRepositoryPort,
        passwordEncoder: PasswordEncoderPort,
        tokenPort: TokenPort,
        authPort: AuthPort,
    ) = AuthUseCase(userRepository, passwordEncoder, tokenPort, authPort)

    @Bean
    fun aiUseCase(
        aiPort: AiPort,
        embeddingPort: EmbeddingPort,
        vectorSearchPort: VectorSearchPort,
        documentRepository: DocumentRepositoryPort,
        processPort: DocumentProcessPort,
        queryLogPort: QueryLogPort,
    ) = AiUseCase(aiPort, embeddingPort, vectorSearchPort, documentRepository, processPort, queryLogPort)

    @Bean
    fun searchUseCase(strategies: List<SearchStrategy>) = SearchUseCase(strategies)

    @Bean
    fun adminUseCase(
        documentRepository: DocumentRepositoryPort,
        queryLogPort: QueryLogPort,
        processPort: DocumentProcessPort,
        documentJobPort: DocumentJobPort,
    ) = AdminUseCase(documentRepository, queryLogPort, processPort, documentJobPort)
}
