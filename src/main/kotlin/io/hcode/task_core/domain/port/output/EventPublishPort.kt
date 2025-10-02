package io.hcode.task_core.domain.port.output

import io.hcode.task_core.domain.model.GanttTask
import io.hcode.task_core.domain.model.KanbanCard
import io.hcode.task_core.domain.model.Todo

/**
 * 이벤트 발행 포트 (Outbound Port)
 * 
 * 비즈니스 의미: 도메인 이벤트를 외부(SSE 클라이언트)로 발행하는 외부 어댑터의 계약을 정의합니다.
 * 헥사고날 아키텍처: 도메인 계층이 인프라 계층에 의존하지 않도록 추상화를 제공합니다.
 * 
 * 구현: SSE(Server-Sent Events) 어댑터가 이 인터페이스를 구현합니다.
 * 
 * 비즈니스 규칙:
 * - 트랜잭션 커밋 이후에 이벤트를 발행합니다.
 * - 최소 한 번 통지(at-least-once delivery)를 보장합니다.
 */
interface EventPublishPort {
    
    /**
     * Todo 생성 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 새로운 Todo가 생성되었음을 구독자들에게 알립니다.
     * 
     * @param todo 생성된 Todo 엔티티
     */
    suspend fun publishTodoCreated(todo: Todo)
    
    /**
     * Todo 수정 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: Todo가 업데이트되었음을 구독자들에게 알립니다.
     * 
     * @param todo 수정된 Todo 엔티티
     */
    suspend fun publishTodoUpdated(todo: Todo)
    
    /**
     * Todo 삭제 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: Todo가 소프트 삭제되었음을 구독자들에게 알립니다.
     * 
     * @param todo 삭제된 Todo 엔티티
     */
    suspend fun publishTodoDeleted(todo: Todo)
    
    /**
     * Todo 복구 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 소프트 삭제된 Todo가 복구되었음을 구독자들에게 알립니다.
     * 
     * @param todo 복구된 Todo 엔티티
     */
    suspend fun publishTodoRestored(todo: Todo)
    
    /**
     * GanttTask 생성 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 새로운 간트 태스크가 생성되었음을 구독자들에게 알립니다.
     * 
     * @param ganttTask 생성된 GanttTask 엔티티
     */
    suspend fun publishGanttTaskCreated(ganttTask: GanttTask)
    
    /**
     * GanttTask 수정 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 간트 태스크가 업데이트되었음을 구독자들에게 알립니다.
     * 
     * @param ganttTask 수정된 GanttTask 엔티티
     */
    suspend fun publishGanttTaskUpdated(ganttTask: GanttTask)
    
    /**
     * GanttTask 삭제 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 간트 태스크가 소프트 삭제되었음을 구독자들에게 알립니다.
     * 
     * @param ganttTask 삭제된 GanttTask 엔티티
     */
    suspend fun publishGanttTaskDeleted(ganttTask: GanttTask)
    
    /**
     * GanttTask 복구 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 소프트 삭제된 간트 태스크가 복구되었음을 구독자들에게 알립니다.
     * 
     * @param ganttTask 복구된 GanttTask 엔티티
     */
    suspend fun publishGanttTaskRestored(ganttTask: GanttTask)
    
    /**
     * KanbanCard 생성 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 새로운 칸반 카드가 생성되었음을 구독자들에게 알립니다.
     * 
     * @param kanbanCard 생성된 KanbanCard 엔티티
     */
    suspend fun publishKanbanCardCreated(kanbanCard: KanbanCard)
    
    /**
     * KanbanCard 이동 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 칸반 카드가 다른 컬럼으로 이동하거나 위치가 변경되었음을 구독자들에게 알립니다.
     * 
     * @param kanbanCard 이동된 KanbanCard 엔티티
     * @param previousColumnId 이전 컬럼 ID
     * @param previousPosition 이전 위치
     */
    suspend fun publishKanbanCardMoved(
        kanbanCard: KanbanCard,
        previousColumnId: String,
        previousPosition: Int
    )
    
    /**
     * KanbanCard 삭제 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 칸반 카드가 소프트 삭제되었음을 구독자들에게 알립니다.
     * 
     * @param kanbanCard 삭제된 KanbanCard 엔티티
     */
    suspend fun publishKanbanCardDeleted(kanbanCard: KanbanCard)
    
    /**
     * KanbanCard 복구 이벤트를 발행합니다.
     * 
     * 비즈니스 의미: 소프트 삭제된 칸반 카드가 복구되었음을 구독자들에게 알립니다.
     * 
     * @param kanbanCard 복구된 KanbanCard 엔티티
     */
    suspend fun publishKanbanCardRestored(kanbanCard: KanbanCard)
}
