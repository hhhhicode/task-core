package io.hcode.task_core.application.service

import io.hcode.task_core.domain.port.input.GanttQueryPort
import io.hcode.task_core.domain.port.output.GanttStorePort
import io.hcode.task_core.domain.model.GanttTask
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 간트 차트 조회 서비스 (CQRS - Query Side)
 *
 * 비즈니스 의미: 간트 차트의 일정 계획 데이터에 대한 모든 읽기 연산을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class GanttQueryService(
    private val ganttStorePort: GanttStorePort
) : GanttQueryPort {

    /**
     * ID로 간트 태스크 조회
     *
     * 비즈니스 의미: 삭제 여부를 선택적으로 포함하여 간트 태스크를 식별자로 조회합니다.
     */
    override suspend fun getGanttTaskById(id: UUID, includeDeleted: Boolean): GanttTask? {
        return ganttStorePort.findById(id, includeDeleted)
    }

    /**
     * Todo ID로 간트 태스크 조회
     *
     * 비즈니스 의미: 특정 Todo와 연결된 일정 계획을 조회합니다.
     */
    override suspend fun getGanttTaskByTodoId(todoId: UUID, includeDeleted: Boolean): GanttTask? {
        return if (includeDeleted) {
            // includeDeleted=true일 경우 직접 쿼리 필요 (StorePort에 메서드가 없으므로)
            // 임시로 findByTodoIdActive를 사용하고, 향후 StorePort에 메서드 추가 필요
            ganttStorePort.findByTodoIdActive(todoId)
        } else {
            ganttStorePort.findByTodoIdActive(todoId)
        }
    }

    /**
     * 모든 간트 태스크 조회
     *
     * 비즈니스 의미: 삭제 여부를 선택적으로 포함하여 모든 간트 태스크를 조회합니다.
     */
    override fun getAllGanttTasks(includeDeleted: Boolean): Flow<GanttTask> {
        return ganttStorePort.findAll(includeDeleted)
    }

    /**
     * 여러 Todo ID로 간트 태스크 일괄 조회
     *
     * 비즈니스 의미: 여러 Todo의 일정 계획을 효율적으로 조회합니다.
     */
    override fun getGanttTasksByTodoIds(todoIds: Collection<UUID>): Flow<GanttTask> {
        return ganttStorePort.findByTodoIdsActive(todoIds)
    }
}
