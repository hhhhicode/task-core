package io.hcode.task_core.application.service

import io.hcode.task_core.domain.port.input.KanbanQueryPort
import io.hcode.task_core.domain.port.output.KanbanStorePort
import io.hcode.task_core.domain.model.KanbanCard
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 칸반 보드 조회 서비스 (CQRS - Query Side)
 *
 * 비즈니스 의미: 칸반 보드의 워크플로우 데이터에 대한 모든 읽기 연산을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class KanbanQueryService(
    private val kanbanStorePort: KanbanStorePort
) : KanbanQueryPort {

    /**
     * ID로 칸반 카드 조회
     *
     * 비즈니스 의미: 삭제 여부를 선택적으로 포함하여 칸반 카드를 식별자로 조회합니다.
     */
    override suspend fun getKanbanCardById(id: UUID, includeDeleted: Boolean): KanbanCard? {
        return kanbanStorePort.findById(id, includeDeleted)
    }

    /**
     * Todo ID로 칸반 카드 조회
     *
     * 비즈니스 의미: 특정 Todo의 워크플로우 위치를 조회합니다.
     */
    override suspend fun getKanbanCardByTodoId(todoId: UUID, includeDeleted: Boolean): KanbanCard? {
        return if (includeDeleted) {
            // includeDeleted=true일 경우 직접 쿼리 필요 (StorePort에 메서드가 없으므로)
            // 임시로 findByIdActive를 사용하고, 향후 StorePort에 메서드 추가 필요
            kanbanStorePort.findByTodoIdActive(todoId)
        } else {
            kanbanStorePort.findByTodoIdActive(todoId)
        }
    }

    /**
     * 특정 컬럼의 칸반 카드 조회
     *
     * 비즈니스 의미: 특정 워크플로우 단계에 있는 모든 활성 카드를 조회합니다.
     */
    override fun getKanbanCardsByColumn(columnId: String): Flow<KanbanCard> {
        return kanbanStorePort.findByColumnIdActive(columnId)
    }

    /**
     * 여러 컬럼의 칸반 카드 조회
     *
     * 비즈니스 의미: 여러 워크플로우 단계의 모든 활성 카드를 조회합니다.
     */
    override fun getKanbanCardsByColumns(columnIds: Collection<String>): Flow<KanbanCard> {
        return kanbanStorePort.findByColumnIdsActive(columnIds)
    }

    /**
     * 모든 칸반 카드 조회
     *
     * 비즈니스 의미: 삭제 여부를 선택적으로 포함하여 모든 칸반 카드를 조회합니다.
     */
    override fun getAllKanbanCards(includeDeleted: Boolean): Flow<KanbanCard> {
        return kanbanStorePort.findAll(includeDeleted)
    }
}
