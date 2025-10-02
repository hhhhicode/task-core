package io.hcode.task_core.domain.port.input

import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.UpdateTodoRequest
import io.hcode.task_core.domain.model.Todo
import java.util.UUID

/**
 * Todo 커맨드 포트 (Inbound Port)
 * 
 * 비즈니스 의미: Todo 엔티티의 상태 변경(생성, 수정, 삭제, 복구)을 담당하는 유스케이스입니다.
 * 헥사고날 아키텍처: 애플리케이션의 진입점으로, 외부(웹 어댑터)에서 도메인 로직을 호출합니다.
 * 
 * CQRS: Command(명령) 전용 포트로, 상태 변경 작업만 담당합니다.
 */
interface TodoCommandPort {
    
    /**
     * 새로운 Todo를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - 제목은 필수이며 빈 문자열일 수 없습니다.
     * - 우선순위 기본값은 MEDIUM입니다.
     * - 상태는 TODO로 시작합니다.
     * - 생성 시 SSE 이벤트를 발행합니다.
     * 
     * @param request Todo 생성 요청
     * @return 생성된 Todo 엔티티
     */
    suspend fun createTodo(request: CreateTodoRequest): Todo
    
    /**
     * 기존 Todo를 업데이트합니다.
     * 
     * 비즈니스 규칙:
     * - 활성 상태(deletedAt IS NULL)의 Todo만 수정 가능합니다.
     * - 제공된 필드만 업데이트됩니다 (부분 업데이트).
     * - updatedAt은 현재 시각으로 갱신됩니다.
     * - 수정 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 업데이트할 Todo의 ID
     * @param request Todo 수정 요청
     * @return 업데이트된 Todo 엔티티
     * @throws NoSuchElementException Todo가 존재하지 않거나 삭제된 경우
     */
    suspend fun updateTodo(id: UUID, request: UpdateTodoRequest): Todo
    
    /**
     * Todo를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 현재 시각으로 설정합니다.
     * - 연결된 GanttTask와 KanbanCard도 함께 소프트 삭제됩니다.
     * - 삭제 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 삭제할 Todo의 ID
     * @throws NoSuchElementException Todo가 존재하지 않는 경우
     */
    suspend fun deleteTodo(id: UUID)
    
    /**
     * 소프트 삭제된 Todo를 복구합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 NULL로 설정합니다.
     * - 연결된 GanttTask와 KanbanCard도 함께 복구됩니다.
     * - updatedAt은 현재 시각으로 갱신됩니다.
     * - 복구 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 복구할 Todo의 ID
     * @return 복구된 Todo 엔티티
     * @throws NoSuchElementException Todo가 존재하지 않는 경우
     */
    suspend fun restoreTodo(id: UUID): Todo
}
