package io.hcode.task_core.application.service

import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.UpdateTodoRequest
import io.hcode.task_core.domain.model.Todo
import io.hcode.task_core.domain.port.input.TodoCommandPort
import io.hcode.task_core.domain.port.output.EventPublishPort
import io.hcode.task_core.domain.port.output.GanttStorePort
import io.hcode.task_core.domain.port.output.KanbanStorePort
import io.hcode.task_core.domain.port.output.TodoStorePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Todo 명령 서비스 (Application Service)
 *
 * 비즈니스 의미: Todo 생성, 수정, 삭제 등의 비즈니스 로직을 처리합니다.
 *
 * CQRS 패턴:
 * - 명령(Command) 책임만 가집니다.
 * - 조회(Query)는 TodoQueryService에서 담당합니다.
 * - 상태 변경 작업만 수행합니다.
 *
 * 기술적 배경:
 * - Hexagonal Architecture의 Application Layer에 위치합니다.
 * - Inbound Port (TodoCommandPort)를 구현합니다.
 * - Outbound Port (TodoStorePort, EventPublishPort 등)를 사용합니다.
 * - 트랜잭션 경계를 관리합니다.
 */
@Service
@Transactional
class TodoCommandService(
    private val todoStorePort: TodoStorePort,
    private val ganttStorePort: GanttStorePort,
    private val kanbanStorePort: KanbanStorePort,
    private val eventPublishPort: EventPublishPort
) : TodoCommandPort {

    override suspend fun createTodo(request: CreateTodoRequest): Todo {
        val now = Instant.now()

        // 도메인 엔티티 생성
        val todo = Todo(
            id = UUID.randomUUID(),
            title = request.title!!,
            description = request.description,
            status = request.status!!,
            priority = request.priority!!,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        // 영속화
        todoStorePort.create(todo)

        // 도메인 이벤트 발행
        eventPublishPort.publishTodoCreated(todo)

        return todo
    }

    override suspend fun updateTodo(id: UUID, request: UpdateTodoRequest): Todo {
        val now = Instant.now()

        // 기존 Todo 조회
        val existingTodo = todoStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("Todo not found or deleted: $id")

        // 도메인 로직을 사용하여 업데이트
        val updatedTodo = existingTodo.update(
            title = request.title,
            description = request.description,
            status = request.status,
            priority = request.priority,
            updatedAt = now
        )

        // 영속화
        todoStorePort.update(updatedTodo)

        // 도메인 이벤트 발행
        eventPublishPort.publishTodoUpdated(updatedTodo)

        return updatedTodo
    }

    override suspend fun deleteTodo(id: UUID) {
        // 삭제 전에 Todo 조회 (이벤트 발행용)
        val todo = todoStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("Todo not found: $id")

        // Todo 소프트 삭제
        todoStorePort.softDelete(id)

        // 연결된 GanttTask 소프트 삭제
        ganttStorePort.softDeleteByTodoId(id)

        // 연결된 KanbanCard 소프트 삭제
        kanbanStorePort.softDeleteByTodoId(id)

        // 도메인 이벤트 발행
        eventPublishPort.publishTodoDeleted(todo)
    }

    override suspend fun restoreTodo(id: UUID): Todo {
        // 삭제된 Todo 조회 (삭제된 것도 포함)
        val deletedTodo = todoStorePort.findById(id, includeDeleted = true)
            ?: throw IllegalArgumentException("Todo not found: $id")

        if (deletedTodo.isActive()) {
            throw IllegalStateException("Todo is already active: $id")
        }

        val now = Instant.now()

        // 도메인 로직을 사용하여 복구
        val restoredTodo = deletedTodo.restore(restoredAt = now)

        // 영속화
        todoStorePort.update(restoredTodo)

        // 연결된 GanttTask 복구
        ganttStorePort.restoreByTodoId(id)

        // 연결된 KanbanCard 복구
        kanbanStorePort.restoreByTodoId(id)

        // 도메인 이벤트 발행
        eventPublishPort.publishTodoRestored(restoredTodo)

        return restoredTodo
    }
}
