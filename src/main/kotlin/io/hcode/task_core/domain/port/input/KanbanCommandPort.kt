package io.hcode.task_core.domain.port.input

import io.hcode.task_core.adapter.input.web.dto.kanban.CreateKanbanCardRequest
import io.hcode.task_core.adapter.input.web.dto.kanban.MoveKanbanCardRequest
import io.hcode.task_core.domain.model.KanbanCard
import java.util.UUID

/**
 * KanbanCard 커맨드 포트 (Inbound Port)
 * 
 * 비즈니스 의미: KanbanCard 엔티티의 상태 변경(생성, 이동, 삭제, 복구)을 담당하는 유스케이스입니다.
 * 헥사고날 아키텍처: 애플리케이션의 진입점으로, 외부(웹 어댑터)에서 도메인 로직을 호출합니다.
 * 
 * CQRS: Command(명령) 전용 포트로, 상태 변경 작업만 담당합니다.
 */
interface KanbanCommandPort {
    
    /**
     * 새로운 KanbanCard를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - todoId는 활성 상태의 Todo여야 합니다.
     * - 하나의 Todo는 최대 하나의 KanbanCard를 가질 수 있습니다.
     * - columnId는 빈 문자열일 수 없습니다.
     * - position은 0 이상이어야 합니다.
     * - 생성 시 SSE 이벤트를 발행합니다.
     * 
     * @param request KanbanCard 생성 요청
     * @return 생성된 KanbanCard 엔티티
     * @throws NoSuchElementException Todo가 존재하지 않는 경우
     * @throws IllegalStateException Todo에 이미 KanbanCard가 존재하는 경우
     */
    suspend fun createKanbanCard(request: CreateKanbanCardRequest): KanbanCard
    
    /**
     * KanbanCard를 다른 컬럼으로 이동하거나 같은 컬럼 내에서 위치를 변경합니다.
     * 
     * 비즈니스 규칙:
     * - 활성 상태(deletedAt IS NULL)의 KanbanCard만 이동 가능합니다.
     * - 동일 컬럼 내 이동: [old, new] 구간의 카드들을 +1/-1 시프트합니다.
     * - 타 컬럼 이동: 대상 컬럼의 position >= new인 카드들을 +1 시프트합니다.
     * - 이동 시 SSE 이벤트를 발행합니다 (이전/새 컬럼 정보 포함).
     * 
     * @param id 이동할 KanbanCard의 ID
     * @param request 칸반 카드 이동 요청
     * @return 이동된 KanbanCard 엔티티
     * @throws NoSuchElementException KanbanCard가 존재하지 않거나 삭제된 경우
     */
    suspend fun moveKanbanCard(id: UUID, request: MoveKanbanCardRequest): KanbanCard
    
    /**
     * KanbanCard를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 현재 시각으로 설정합니다.
     * - 연결된 Todo는 삭제되지 않습니다.
     * - 삭제 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 삭제할 KanbanCard의 ID
     * @throws NoSuchElementException KanbanCard가 존재하지 않는 경우
     */
    suspend fun deleteKanbanCard(id: UUID)
    
    /**
     * 소프트 삭제된 KanbanCard를 복구합니다.
     * 
     * 비즈니스 규칙:
     * - deletedAt을 NULL로 설정합니다.
     * - 복구 시 SSE 이벤트를 발행합니다.
     * 
     * @param id 복구할 KanbanCard의 ID
     * @return 복구된 KanbanCard 엔티티
     * @throws NoSuchElementException KanbanCard가 존재하지 않는 경우
     */
    suspend fun restoreKanbanCard(id: UUID): KanbanCard
}
