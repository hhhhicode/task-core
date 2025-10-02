package io.hcode.task_core.adapter.input.web.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import java.net.URI

/**
 * 전역 예외 핸들러 (Global Exception Handler)
 *
 * 비즈니스 의미: 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리하고 응답합니다.
 *
 * 기술적 배경:
 * - RFC 7807 (Problem Details for HTTP APIs) 준수
 * - Spring Boot 3의 ProblemDetail 사용
 * - 클라이언트에게 기계가 읽을 수 있는 표준화된 에러 응답 제공
 *
 * RFC 7807 필드:
 * - type: 문제 유형을 식별하는 URI
 * - title: 짧은 사람이 읽을 수 있는 요약
 * - status: HTTP 상태 코드
 * - detail: 이 발생에 대한 구체적인 설명
 * - instance: 이 발생을 식별하는 URI
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 리소스를 찾을 수 없는 경우 (404 Not Found)
     *
     * 비즈니스 의미: 요청한 리소스(Todo, GanttTask, KanbanCard)가 존재하지 않습니다.
     *
     * @param ex NoSuchElementException 예외
     * @return RFC 7807 ProblemDetail 응답
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ProblemDetail {
        logger.warn("Resource not found: ${ex.message}")

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found").apply {
            type = URI.create("https://api.task-core.io/problems/not-found")
            title = "Resource Not Found"
        }
    }

    /**
     * 잘못된 요청 상태 (400 Bad Request)
     *
     * 비즈니스 의미: 비즈니스 규칙 위반 또는 잘못된 요청입니다.
     *
     * @param ex IllegalArgumentException 예외
     * @return RFC 7807 ProblemDetail 응답
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ProblemDetail {
        logger.warn("Invalid argument: ${ex.message}")

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request").apply {
            type = URI.create("https://api.task-core.io/problems/invalid-argument")
            title = "Invalid Argument"
        }
    }

    /**
     * 잘못된 상태 (409 Conflict)
     *
     * 비즈니스 의미: 현재 리소스 상태와 충돌하는 작업입니다.
     *
     * @param ex IllegalStateException 예외
     * @return RFC 7807 ProblemDetail 응답
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ProblemDetail {
        logger.warn("Invalid state: ${ex.message}")

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Resource state conflict").apply {
            type = URI.create("https://api.task-core.io/problems/invalid-state")
            title = "Invalid State"
        }
    }

    /**
     * 검증 실패 (400 Bad Request)
     *
     * 비즈니스 의미: DTO 검증 실패 (@Valid 어노테이션으로 인한 검증 오류)
     *
     * @param ex WebExchangeBindException 예외
     * @return RFC 7807 ProblemDetail 응답
     */
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException): ProblemDetail {
        logger.warn("Validation failed: ${ex.bindingResult.fieldErrors}")

        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Validation error")
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for ${ex.bindingResult.objectName}"
        ).apply {
            type = URI.create("https://api.task-core.io/problems/validation-failed")
            title = "Validation Failed"
            setProperty("errors", fieldErrors)
        }
    }

    /**
     * 예상치 못한 서버 오류 (500 Internal Server Error)
     *
     * 비즈니스 의미: 예기치 않은 시스템 오류가 발생했습니다.
     *
     * @param ex Exception 예외
     * @return RFC 7807 ProblemDetail 응답
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ProblemDetail {
        logger.error("Unexpected error occurred", ex)

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
        ).apply {
            type = URI.create("https://api.task-core.io/problems/internal-error")
            title = "Internal Server Error"
        }
    }
}
