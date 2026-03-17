package com.sleekydz86.paperlens.infrastructure.web

import com.sleekydz86.paperlens.application.exception.DuplicateEmailException
import com.sleekydz86.paperlens.application.exception.EmbeddingNotAvailableException
import com.sleekydz86.paperlens.application.exception.InvalidCredentialsException
import org.springframework.http.HttpStatus
import java.util.NoSuchElementException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message ?: "이메일 또는 비밀번호가 올바르지 않습니다."))

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(e: DuplicateEmailException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "이미 등록된 이메일입니다."))

    @ExceptionHandler(EmbeddingNotAvailableException::class)
    fun handleEmbeddingNotAvailable(e: EmbeddingNotAvailableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse(e.message ?: "임베딩이 비활성화되어 있습니다."))

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        val msg = e.message ?: ""
        if (msg.contains("Embedding", ignoreCase = true) || msg.contains("임베딩", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse(if (msg.contains("임베딩")) msg else "임베딩이 비활성화되어 있습니다. 'no-embedding' 프로필을 제거하고 ONNX 모델/토크나이저 URI를 설정하세요.")
            )
        }
        if (msg.contains("파일", ignoreCase = true) || msg.contains("File not found", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(if (msg.contains("파일")) msg else "파일을 찾을 수 없습니다."))
        }
        throw e
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(e: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "요청한 항목을 찾을 수 없습니다."))
}
