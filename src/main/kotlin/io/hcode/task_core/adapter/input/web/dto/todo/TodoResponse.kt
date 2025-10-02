package io.hcode.task_core.adapter.input.web.dto.todo

import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import io.hcode.task_core.domain.model.Todo
import java.time.Instant
import java.util.UUID

/**
 * Todo 조회 응답 DTO
 * 
 * 비즈니스 의미: Todo 작업의 전체 정보를 클라이언트에게 반환합니다.
 */
data class TodoResponse(
    /**
     * 고유 식별자
     */
    val id: UUID,
    
    /**
     * 제목
     */
    val title: String,
    
    /**
     * 설명
     */
    val description: String?,
    
    /**
     * 상태
     */
    val status: TaskStatus,
    
    /**
     * 우선순위
     */
    val priority: TaskPriority,
    
    /**
     * 생성 시각 (UTC)
     */
    val createdAt: Instant,
    
    /**
     * 수정 시각 (UTC)
     */
    val updatedAt: Instant,
    
    /**
     * 소프트 삭제 시각 (NULL = 활성)
     */
    val deletedAt: Instant?
) {
    companion object {
        /**
         * 도메인 모델로부터 DTO를 생성합니다.
         * 비즈니스 규칙: 도메인 계층과 프레젠테이션 계층 간의 변환을 담당합니다.
         */
        fun from(todo: Todo): TodoResponse = TodoResponse(
            id = todo.id,
            title = todo.title,
            description = todo.description,
            status = todo.status,
            priority = todo.priority,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
            deletedAt = todo.deletedAt
        )
    }
}
