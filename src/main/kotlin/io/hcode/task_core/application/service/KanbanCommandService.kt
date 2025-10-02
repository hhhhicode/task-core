package io.hcode.task_core.application.service

import io.hcode.task_core.adapter.input.web.dto.kanban.CreateKanbanCardRequest
import io.hcode.task_core.adapter.input.web.dto.kanban.MoveKanbanCardRequest
import io.hcode.task_core.domain.port.input.KanbanCommandPort
import io.hcode.task_core.domain.port.output.EventPublishPort
import io.hcode.task_core.domain.port.output.KanbanStorePort
import io.hcode.task_core.domain.port.output.TodoStorePort
import io.hcode.task_core.domain.model.KanbanCard
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 칸반 보드 명령 서비스 (CQRS - Command Side)
 *
 * 비즈니스 의미: 칸반 보드에서 Todo의 워크플로우 관리에 대한 모든 쓰기 연산을 처리합니다.
 */
@Service
@Transactional
class KanbanCommandService(
    private val kanbanStorePort: KanbanStorePort,
    private val todoStorePort: TodoStorePort,
    private val eventPublishPort: EventPublishPort
) : KanbanCommandPort {

    /**
     * 칸반 카드 생성
     *
     * 비즈니스 의미: Todo를 칸반 보드의 특정 컬럼에 추가하여 워크플로우를 시작합니다.
     */
    override suspend fun createKanbanCard(request: CreateKanbanCardRequest): KanbanCard {
        // Todo 존재 여부 검증
        val todo = todoStorePort.findByIdActive(request.todoId!!)
            ?: throw NoSuchElementException("Todo not found: ${request.todoId}")

        // 이미 칸반 카드가 존재하는지 검증 (1:1 관계)
        val existing = kanbanStorePort.findByTodoIdActive(request.todoId)
        if (existing != null) {
            throw IllegalStateException("KanbanCard already exists for Todo: ${request.todoId}")
        }

        // 해당 위치 이후의 카드들을 한 칸씩 뒤로 이동
        kanbanStorePort.shiftPositions(request.columnId!!, request.position!!, 1)

        val kanbanCard = KanbanCard(
            id = UUID.randomUUID(),
            todoId = request.todoId,
            columnId = request.columnId,
            position = request.position,
            deletedAt = null
        )

        kanbanStorePort.create(kanbanCard)
        eventPublishPort.publishKanbanCardCreated(kanbanCard)
        return kanbanCard
    }

    /**
     * 칸반 카드 이동
     *
     * 비즈니스 의미: 칸반 카드를 다른 컬럼 또는 다른 위치로 이동하여 워크플로우 진행 상태를 변경합니다.
     */
    override suspend fun moveKanbanCard(id: UUID, request: MoveKanbanCardRequest): KanbanCard {
        val existing = kanbanStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("KanbanCard not found: $id")

        val previousColumnId = existing.columnId
        val previousPosition = existing.position

        val targetColumnId = request.newColumnId
        val targetPosition = request.newPosition

        // 같은 컬럼 내 이동
        if (existing.columnId == targetColumnId) {
            if (existing.position < targetPosition) {
                // 아래로 이동: 기존 위치+1 ~ 목표 위치 사이의 카드들을 위로 이동
                kanbanStorePort.shiftPositions(targetColumnId, existing.position + 1, -1)
            } else if (existing.position > targetPosition) {
                // 위로 이동: 목표 위치 ~ 기존 위치-1 사이의 카드들을 아래로 이동
                kanbanStorePort.shiftPositions(targetColumnId, targetPosition, 1)
            }
        } else {
            // 다른 컬럼으로 이동
            // 1. 기존 컬럼에서 기존 위치 이후의 카드들을 위로 이동
            kanbanStorePort.shiftPositions(existing.columnId, existing.position + 1, -1)
            // 2. 목표 컬럼에서 목표 위치 이후의 카드들을 아래로 이동
            kanbanStorePort.shiftPositions(targetColumnId, targetPosition, 1)
        }

        val moved = existing.copy(
            columnId = targetColumnId,
            position = targetPosition
        )

        kanbanStorePort.update(moved)
        eventPublishPort.publishKanbanCardMoved(moved, previousColumnId, previousPosition)
        return moved
    }

    /**
     * 칸반 카드 삭제 (소프트 삭제)
     *
     * 비즈니스 의미: 칸반 보드에서 Todo를 제거합니다.
     */
    override suspend fun deleteKanbanCard(id: UUID) {
        val kanbanCard = kanbanStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("KanbanCard not found: $id")

        // 삭제된 카드 이후의 카드들을 위로 이동
        kanbanStorePort.shiftPositions(kanbanCard.columnId, kanbanCard.position + 1, -1)

        kanbanStorePort.softDelete(id)
        eventPublishPort.publishKanbanCardDeleted(kanbanCard)
    }

    /**
     * 칸반 카드 복원
     *
     * 비즈니스 의미: 삭제된 칸반 카드를 다시 활성화합니다.
     */
    override suspend fun restoreKanbanCard(id: UUID): KanbanCard {
        // restore 메서드는 Boolean을 반환하므로, 먼저 복원 후 조회해야 합니다
        val restored = kanbanStorePort.restore(id)
        if (!restored) {
            throw IllegalArgumentException("Failed to restore KanbanCard: $id")
        }

        // 복원된 카드 조회
        val kanbanCard = kanbanStorePort.findById(id, includeDeleted = false)
            ?: throw IllegalArgumentException("KanbanCard not found after restore: $id")

        // 복원 시 해당 위치 이후의 카드들을 한 칸씩 뒤로 이동
        kanbanStorePort.shiftPositions(kanbanCard.columnId, kanbanCard.position, 1)

        eventPublishPort.publishKanbanCardRestored(kanbanCard)
        return kanbanCard
    }
}
