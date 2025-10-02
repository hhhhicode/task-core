package io.hcode.task_core.domain.port.input

import io.hcode.task_core.adapter.input.web.dto.gantt.CreateGanttTaskRequest
import io.hcode.task_core.adapter.input.web.dto.gantt.UpdateGanttTaskRequest
import io.hcode.task_core.domain.model.GanttTask
import java.util.UUID

/**
 * GanttTask 커맨드 포트 (Inbound Port)
 * 
 * 비즈니스 의미: GanttTask 엔티티의 상태 변경(생성, 수정, 삭제, 복구)을 담당하는 유스케이스입니다.
 * 헥사고날 아키텍처: 애플리케이션의 진입점으로, 외부(웹 어댑터)에서 도메인 로직을 호출합니다.
 * 
 * CQRS: Command(명령) 전용 포트로, 상태 변경 작업만 담당합니다.
 */
interface GanttCommandPort {
    
    /**
     * 새로운 GanttTask를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - todoId는 활성 상태의 Todo여야 합니다.
     * - 하나의 Todo는 최대 하나의 GanttTask를 가질 수 있습니다.
     * - startDate <= endDate 이어야 합니다.
     * - 진행률은 0-100 범위여야 합니다.
     * - 생성 시 SSE 이벤트를 발행합니다.
     * 
     * @param request GanttTask 생성 요청
     * @return 생성된 GanttTask 엔티티
     * @throws NoSuchElementException Todo가 존재하지 않는 경우
     * @throws IllegalStateException Todo에 이미 GanttTask가 존재하는 경우
     */
    suspend fun createGanttTask(request: CreateGanttTaskRequest): GanttTask
    
    /**
     * 기존 GanttTask를 업데이트합니다.
     * 
     * 비즈니스 규칙:
     * - 활성 상태(deletedAt IS NULL)의 GanttTask만 수정 가능합니다.
     * - 제공된 필드만 업데이트됩니다 (부분 업데이트).
     * - startDate <= endDate 제약을 검증합니다.
     * - 진행률은 0-100 범위여야 합니다.
     * - 수정 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 업데이트할 GanttTask의 ID
     * @param request GanttTask 수정 요청
     * @return 업데이트된 GanttTask 엔티티
     * @throws NoSuchElementException GanttTask가 존재하지 않거나 삭제된 경우
     */
    suspend fun updateGanttTask(id: UUID, request: UpdateGanttTaskRequest): GanttTask
    
    /**
     * GanttTask를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 현재 시각으로 설정합니다.
     * - 연결된 Todo는 삭제되지 않습니다.
     * - 삭제 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 삭제할 GanttTask의 ID
     * @throws NoSuchElementException GanttTask가 존재하지 않는 경우
     */
    suspend fun deleteGanttTask(id: UUID)
    
    /**
     * 소프트 삭제된 GanttTask를 복구합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 NULL로 설정합니다.
     * - 복구 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 복구할 GanttTask의 ID
     * @return 복구된 GanttTask 엔티티
     * @throws NoSuchElementException GanttTask가 존재하지 않는 경우
     */
    suspend fun restoreGanttTask(id: UUID): GanttTask
}
