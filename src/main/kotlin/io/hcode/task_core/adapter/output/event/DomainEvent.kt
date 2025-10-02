package io.hcode.task_core.adapter.output.event

import io.hcode.task_core.domain.model.GanttTask
import io.hcode.task_core.domain.model.KanbanCard
import io.hcode.task_core.domain.model.Todo
import java.time.Instant

/**
 * 도메인 이벤트 (Domain Event)
 *
 * 비즈니스 의미: 시스템에서 발생한 비즈니스 이벤트를 표현합니다.
 * SSE를 통해 클라이언트에게 전달됩니다.
 *
 * 기술적 배경:
 * - Sealed class로 타입 안전성 보장
 * - 모든 이벤트는 타입(type)과 타임스탬프(timestamp)를 포함
 * - JSON 직렬화를 위한 구조
 */
sealed class DomainEvent {
    abstract val type: String
    abstract val timestamp: Instant
    abstract val eventId: Long

    // Todo Events
    data class TodoCreated(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val todo: Todo
    ) : DomainEvent() {
        override val type = "TODO_CREATED"
    }

    data class TodoUpdated(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val todo: Todo
    ) : DomainEvent() {
        override val type = "TODO_UPDATED"
    }

    data class TodoDeleted(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val todo: Todo
    ) : DomainEvent() {
        override val type = "TODO_DELETED"
    }

    data class TodoRestored(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val todo: Todo
    ) : DomainEvent() {
        override val type = "TODO_RESTORED"
    }

    // Gantt Task Events
    data class GanttTaskCreated(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val ganttTask: GanttTask
    ) : DomainEvent() {
        override val type = "GANTT_TASK_CREATED"
    }

    data class GanttTaskUpdated(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val ganttTask: GanttTask
    ) : DomainEvent() {
        override val type = "GANTT_TASK_UPDATED"
    }

    data class GanttTaskDeleted(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val ganttTask: GanttTask
    ) : DomainEvent() {
        override val type = "GANTT_TASK_DELETED"
    }

    data class GanttTaskRestored(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val ganttTask: GanttTask
    ) : DomainEvent() {
        override val type = "GANTT_TASK_RESTORED"
    }

    // Kanban Card Events
    data class KanbanCardCreated(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val kanbanCard: KanbanCard
    ) : DomainEvent() {
        override val type = "KANBAN_CARD_CREATED"
    }

    data class KanbanCardMoved(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val kanbanCard: KanbanCard,
        val previousColumnId: String,
        val previousPosition: Int
    ) : DomainEvent() {
        override val type = "KANBAN_CARD_MOVED"
    }

    data class KanbanCardDeleted(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val kanbanCard: KanbanCard
    ) : DomainEvent() {
        override val type = "KANBAN_CARD_DELETED"
    }

    data class KanbanCardRestored(
        override val eventId: Long,
        override val timestamp: Instant = Instant.now(),
        val kanbanCard: KanbanCard
    ) : DomainEvent() {
        override val type = "KANBAN_CARD_RESTORED"
    }
}
