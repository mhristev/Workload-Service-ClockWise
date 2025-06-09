package com.clockwise.workloadservice.exception

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException::class)
    suspend fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> = coroutineScope {
        val errorResponse = async {
            ErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = ex.message ?: "Invalid request state"
            )
        }
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.await())
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> = coroutineScope {
        val errorResponse = async {
            ErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = "An unexpected error occurred"
            )
        }
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.await())
    }
}

data class ErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String
) 