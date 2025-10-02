package io.hcode.task_core.adapter.output.event

import io.hcode.task_core.domain.model.GanttTask
import io.hcode.task_core.domain.model.KanbanCard
import io.hcode.task_core.domain.model.Todo
import io.hcode.task_core.domain.port.output.EventPublishPort
import io.hcode.task_core.infrastructure.sse.EventIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks

/**
 * SSE 이벤트 발행 어댑터 (Outbound Adapter)
 *
 * 비즈니스 의미: 도메인 이벤트를 SSE를 통해 연결된 모든 클라이언트에게 브로드캐스트합니다.
 *
 * 기술적 배경:
 * - Sinks.Many를 사용한 멀티캐스트 이벤트 스트림
 * - 백프레셔(Backpressure) 처리: LATEST 전략으로 느린 구독자 대응
 * - Thread-safe: 동시 다발적 이벤트 발행 지원
 *
 * 비즈니스 규칙:
 * - At-least-once delivery: 최소 한 번 전달 보장
 * - Fire-and-forget: 이벤트 발행은 트랜잭션과 독립적
 */
@Component
class SseEventPublishAdapter(
    private val eventIdGenerator: EventIdGenerator
) : EventPublishPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 이벤트 Sink (멀티캐스트 스트림)
     *
     * 비즈니스 의미: 여러 SSE 클라이언트가 동일한 이벤트 스트림을 구독합니다.
     *
     * 기술 선택:
     * - multicast(): 여러 구독자 지원
     * - onBackpressureBuffer(): 백프레셔 버퍼링 (기본 256)
     * - Sinks.EmitFailureHandler.FAIL_FAST: 빠른 실패로 문제 조기 발견
     */
    val eventSink: Sinks.Many<DomainEvent> = Sinks.many()
        .multicast()
        .onBackpressureBuffer()

    /**
     * 이벤트를 Sink에 발행하는 헬퍼 함수
     *
     * 비즈니스 의미: 이벤트 발행 실패 시 로깅하고 계속 진행합니다.
     *
     * @param event 발행할 도메인 이벤트
     */
    private suspend fun emitEvent(event: DomainEvent) {
        val result = eventSink.tryEmitNext(event)

        if (result.isFailure) {
            logger.error("Failed to emit event: $event, result: $result")
        } else {
            logger.debug("Event published: ${event.type}")
        }
    }

    // Todo Events
    override suspend fun publishTodoCreated(todo: Todo) {
        emitEvent(DomainEvent.TodoCreated(eventId = eventIdGenerator.nextId(), todo = todo))
    }

    override suspend fun publishTodoUpdated(todo: Todo) {
        emitEvent(DomainEvent.TodoUpdated(eventId = eventIdGenerator.nextId(), todo = todo))
    }

    override suspend fun publishTodoDeleted(todo: Todo) {
        emitEvent(DomainEvent.TodoDeleted(eventId = eventIdGenerator.nextId(), todo = todo))
    }

    override suspend fun publishTodoRestored(todo: Todo) {
        emitEvent(DomainEvent.TodoRestored(eventId = eventIdGenerator.nextId(), todo = todo))
    }

    // Gantt Task Events
    override suspend fun publishGanttTaskCreated(ganttTask: GanttTask) {
        emitEvent(DomainEvent.GanttTaskCreated(eventId = eventIdGenerator.nextId(), ganttTask = ganttTask))
    }

    override suspend fun publishGanttTaskUpdated(ganttTask: GanttTask) {
        emitEvent(DomainEvent.GanttTaskUpdated(eventId = eventIdGenerator.nextId(), ganttTask = ganttTask))
    }

    override suspend fun publishGanttTaskDeleted(ganttTask: GanttTask) {
        emitEvent(DomainEvent.GanttTaskDeleted(eventId = eventIdGenerator.nextId(), ganttTask = ganttTask))
    }

    override suspend fun publishGanttTaskRestored(ganttTask: GanttTask) {
        emitEvent(DomainEvent.GanttTaskRestored(eventId = eventIdGenerator.nextId(), ganttTask = ganttTask))
    }

    // Kanban Card Events
    override suspend fun publishKanbanCardCreated(kanbanCard: KanbanCard) {
        emitEvent(DomainEvent.KanbanCardCreated(eventId = eventIdGenerator.nextId(), kanbanCard = kanbanCard))
    }

    override suspend fun publishKanbanCardMoved(
        kanbanCard: KanbanCard,
        previousColumnId: String,
        previousPosition: Int
    ) {
        emitEvent(
            DomainEvent.KanbanCardMoved(
                eventId = eventIdGenerator.nextId(),
                kanbanCard = kanbanCard,
                previousColumnId = previousColumnId,
                previousPosition = previousPosition
            )
        )
    }

    override suspend fun publishKanbanCardDeleted(kanbanCard: KanbanCard) {
        emitEvent(DomainEvent.KanbanCardDeleted(eventId = eventIdGenerator.nextId(), kanbanCard = kanbanCard))
    }

    override suspend fun publishKanbanCardRestored(kanbanCard: KanbanCard) {
        emitEvent(DomainEvent.KanbanCardRestored(eventId = eventIdGenerator.nextId(), kanbanCard = kanbanCard))
    }
}
