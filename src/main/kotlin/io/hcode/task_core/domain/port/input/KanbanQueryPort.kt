package io.hcode.task_core.domain.port.input

import io.hcode.task_core.domain.model.KanbanCard
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * KanbanCard 쿼리 포트 (Inbound Port)
 * 
 * 비즈니스 의미: KanbanCard 엔티티의 조회를 담당하는 유스케이스입니다.
 * 헥사고날 아키텍처: 애플리케이션의 진입점으로, 외부(웹 어댑터)에서 도메인 로직을 호출합니다.
 * 
 * CQRS: Query(조회) 전용 포트로, 상태 변경 없이 데이터 조회만 담당합니다.
 */
interface KanbanQueryPort {
    
    /**
     * ID로 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 기본적으로 활성 상태(deletedAt IS NULL)의 KanbanCard만 조회합니다.
     * - includeDeleted=true인 경우 삭제된 KanbanCard도 조회합니다.
     * 
     * @param id 조회할 KanbanCard의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return KanbanCard 엔티티, 존재하지 않으면 null
     */
    suspend fun getKanbanCardById(id: UUID, includeDeleted: Boolean = false): KanbanCard?
    
    /**
     * Todo ID로 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 하나의 Todo는 최대 하나의 KanbanCard를 가집니다.
     * - 기본적으로 활성 상태(deletedAt IS NULL)의 KanbanCard만 조회합니다.
     * 
     * @param todoId 조회할 Todo의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return KanbanCard 엔티티, 존재하지 않으면 null
     */
    suspend fun getKanbanCardByTodoId(todoId: UUID, includeDeleted: Boolean = false): KanbanCard?
    
    /**
     * 특정 컬럼의 KanbanCard들을 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 활성 상태(deletedAt IS NULL)의 KanbanCard만 조회합니다.
     * - position 오름차순으로 정렬됩니다.
     * 
     * @param columnId 조회할 컬럼 ID
     * @return KanbanCard 엔티티의 Flow (position 오름차순)
     */
    fun getKanbanCardsByColumn(columnId: String): Flow<KanbanCard>
    
    /**
     * 여러 컬럼의 KanbanCard들을 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 활성 상태(deletedAt IS NULL)의 KanbanCard만 조회합니다.
     * - columnId, position 순서로 정렬됩니다.
     * 
     * @param columnIds 조회할 컬럼 ID 목록
     * @return KanbanCard 엔티티의 Flow (columnId, position 순서)
     */
    fun getKanbanCardsByColumns(columnIds: Collection<String>): Flow<KanbanCard>
    
    /**
     * 모든 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 기본적으로 활성 상태(deletedAt IS NULL)의 KanbanCard만 조회합니다.
     * - includeDeleted=true인 경우 삭제된 KanbanCard도 조회합니다.
     * - columnId, position 순서로 정렬됩니다.
     * 
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return KanbanCard 엔티티의 Flow
     */
    fun getAllKanbanCards(includeDeleted: Boolean = false): Flow<KanbanCard>
}
