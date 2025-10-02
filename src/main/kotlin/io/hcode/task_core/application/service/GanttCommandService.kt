package io.hcode.task_core.application.service

import io.hcode.task_core.adapter.input.web.dto.gantt.CreateGanttTaskRequest
import io.hcode.task_core.adapter.input.web.dto.gantt.UpdateGanttTaskRequest
import io.hcode.task_core.domain.port.input.GanttCommandPort
import io.hcode.task_core.domain.port.output.EventPublishPort
import io.hcode.task_core.domain.port.output.GanttStorePort
import io.hcode.task_core.domain.port.output.TodoStorePort
import io.hcode.task_core.domain.model.GanttTask
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 간트 차트 명령 서비스 (CQRS - Command Side)
 *
 * 비즈니스 의미: 간트 차트에서 Todo의 일정 계획 관리에 대한 모든 쓰기 연산을 처리합니다.
 */
@Service
@Transactional
class GanttCommandService(
    private val ganttStorePort: GanttStorePort,
    private val todoStorePort: TodoStorePort,
    private val eventPublishPort: EventPublishPort
) : GanttCommandPort {

    /**
     * 간트 태스크 생성
     *
     * 비즈니스 의미: Todo를 간트 차트에 추가하여 일정 계획을 수립합니다.
     */
    override suspend fun createGanttTask(request: CreateGanttTaskRequest): GanttTask {
        // Todo 존재 여부 검증
        val todo = todoStorePort.findByIdActive(request.todoId!!)
            ?: throw NoSuchElementException("Todo not found: ${request.todoId}")

        // 이미 간트 태스크가 존재하는지 검증 (1:1 관계)
        val existing = ganttStorePort.findByTodoIdActive(request.todoId)
        if (existing != null) {
            throw IllegalStateException("GanttTask already exists for Todo: ${request.todoId}")
        }

        val ganttTask = GanttTask(
            id = UUID.randomUUID(),
            todoId = request.todoId,
            startDate = request.startDate!!,
            endDate = request.endDate!!,
            progress = request.progress,
            deletedAt = null
        )

        ganttStorePort.create(ganttTask)
        eventPublishPort.publishGanttTaskCreated(ganttTask)
        return ganttTask
    }

    /**
     * 간트 태스크 수정
     *
     * 비즈니스 의미: 기존 간트 태스크의 일정 계획 정보를 변경합니다.
     */
    override suspend fun updateGanttTask(id: UUID, request: UpdateGanttTaskRequest): GanttTask {
        val existing = ganttStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("GanttTask not found: $id")

        val updated = existing.copy(
            startDate = request.startDate ?: existing.startDate,
            endDate = request.endDate ?: existing.endDate,
            progress = request.progress ?: existing.progress
        )

        ganttStorePort.update(updated)
        eventPublishPort.publishGanttTaskUpdated(updated)
        return updated
    }

    /**
     * 간트 태스크 삭제 (소프트 삭제)
     *
     * 비즈니스 의미: 간트 차트에서 Todo의 일정 계획을 제거합니다.
     */
    override suspend fun deleteGanttTask(id: UUID) {
        val ganttTask = ganttStorePort.findByIdActive(id)
            ?: throw IllegalArgumentException("GanttTask not found: $id")

        ganttStorePort.softDelete(id)
        eventPublishPort.publishGanttTaskDeleted(ganttTask)
    }

    /**
     * 간트 태스크 복원
     *
     * 비즈니스 의미: 삭제된 간트 태스크를 다시 활성화합니다.
     */
    override suspend fun restoreGanttTask(id: UUID): GanttTask {
        val ganttTask = ganttStorePort.findById(id, includeDeleted = true)
            ?: throw IllegalArgumentException("GanttTask not found: $id")

        if (ganttTask.deletedAt == null) {
            throw IllegalStateException("GanttTask is not deleted: $id")
        }

        val restored = ganttTask.copy(deletedAt = null)
        ganttStorePort.update(restored)
        eventPublishPort.publishGanttTaskRestored(restored)
        return restored
    }
}
