package io.hcode.task_core.adapter.input.web.dto.gantt

import io.hcode.task_core.domain.model.GanttTask
import java.time.Instant
import java.util.UUID

/**
 * 간트 태스크 조회 응답 DTO
 * 
 * 비즈니스 의미: 간트 차트 상의 태스크 정보를 클라이언트에게 반환합니다.
 */
data class GanttTaskResponse(
    /**
     * 고유 식별자
     */
    val id: UUID,
    
    /**
     * 연결된 Todo ID
     */
    val todoId: UUID,
    
    /**
     * 시작 일시 (UTC)
     */
    val startDate: Instant,
    
    /**
     * 종료 일시 (UTC)
     */
    val endDate: Instant,
    
    /**
     * 진행률 (0-100)
     */
    val progress: Int,
    
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
        fun from(ganttTask: GanttTask): GanttTaskResponse = GanttTaskResponse(
            id = ganttTask.id,
            todoId = ganttTask.todoId,
            startDate = ganttTask.startDate,
            endDate = ganttTask.endDate,
            progress = ganttTask.progress,
            deletedAt = ganttTask.deletedAt
        )
    }
}
