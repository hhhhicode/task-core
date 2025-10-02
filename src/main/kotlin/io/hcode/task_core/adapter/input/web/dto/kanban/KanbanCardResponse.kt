package io.hcode.task_core.adapter.input.web.dto.kanban

import io.hcode.task_core.domain.model.KanbanCard
import java.time.Instant
import java.util.UUID

/**
 * 칸반 카드 조회 응답 DTO
 * 
 * 비즈니스 의미: 칸반 보드 상의 카드 정보를 클라이언트에게 반환합니다.
 */
data class KanbanCardResponse(
    /**
     * 고유 식별자
     */
    val id: UUID,
    
    /**
     * 연결된 Todo ID
     */
    val todoId: UUID,
    
    /**
     * 칸반 컬럼 ID
     */
    val columnId: String,
    
    /**
     * 컬럼 내 위치
     */
    val position: Int,
    
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
        fun from(kanbanCard: KanbanCard): KanbanCardResponse = KanbanCardResponse(
            id = kanbanCard.id,
            todoId = kanbanCard.todoId,
            columnId = kanbanCard.columnId,
            position = kanbanCard.position,
            deletedAt = kanbanCard.deletedAt
        )
    }
}
