package io.hcode.task_core.domain.port.output

import io.hcode.task_core.domain.model.KanbanCard
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * KanbanCard 영속성 포트 (Outbound Port)
 * 
 * 비즈니스 의미: KanbanCard 엔티티의 영속성 관리를 담당하는 외부 어댑터의 계약을 정의합니다.
 * 헥사고날 아키텍처: 도메인 계층이 인프라 계층에 의존하지 않도록 추상화를 제공합니다.
 * 
 * 구현: jOOQ 기반 어댑터가 이 인터페이스를 구현합니다.
 */
interface KanbanStorePort {
    
    /**
     * 새로운 KanbanCard를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - todoId는 유일해야 합니다 (하나의 Todo는 최대 하나의 KanbanCard).
     * - UUID는 애플리케이션 레이어에서 생성됩니다.
     * 
     * @param kanbanCard 생성할 KanbanCard 엔티티
     * @return 생성된 KanbanCard의 ID
     */
    suspend fun create(kanbanCard: KanbanCard): UUID
    
    /**
     * ID로 활성 상태의 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param id 조회할 KanbanCard의 ID
     * @return KanbanCard 엔티티, 존재하지 않거나 삭제된 경우 null
     */
    suspend fun findByIdActive(id: UUID): KanbanCard?
    
    /**
     * ID로 KanbanCard를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * @param id 조회할 KanbanCard의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return KanbanCard 엔티티, 존재하지 않으면 null
     */
    suspend fun findById(id: UUID, includeDeleted: Boolean = false): KanbanCard?
    
    /**
     * Todo ID로 활성 상태의 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙: 
     * - 하나의 Todo는 최대 하나의 KanbanCard를 가집니다.
     * - deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param todoId 조회할 Todo의 ID
     * @return KanbanCard 엔티티, 존재하지 않거나 삭제된 경우 null
     */
    suspend fun findByTodoIdActive(todoId: UUID): KanbanCard?
    
    /**
     * 특정 컬럼의 활성 상태 KanbanCard들을 조회합니다.
     * 
     * 비즈니스 규칙: 
     * - deletedAt IS NULL인 레코드만 조회합니다.
     * - position 순서로 정렬하여 반환합니다.
     * 
     * @param columnId 조회할 컬럼 ID
     * @return KanbanCard 엔티티의 Flow (position 오름차순)
     */
    fun findByColumnIdActive(columnId: String): Flow<KanbanCard>
    
    /**
     * 여러 컬럼의 활성 상태 KanbanCard들을 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param columnIds 조회할 컬럼 ID 목록
     * @return KanbanCard 엔티티의 Flow (columnId, position 순서)
     */
    fun findByColumnIdsActive(columnIds: Collection<String>): Flow<KanbanCard>
    
    /**
     * 모든 활성 상태의 KanbanCard를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @return KanbanCard 엔티티의 Flow
     */
    fun findAllActive(): Flow<KanbanCard>
    
    /**
     * 모든 KanbanCard를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return KanbanCard 엔티티의 Flow
     */
    fun findAll(includeDeleted: Boolean = false): Flow<KanbanCard>
    
    /**
     * KanbanCard를 업데이트합니다.
     * 
     * 비즈니스 규칙: ID와 todoId는 변경할 수 없습니다.
     * 
     * @param kanbanCard 업데이트할 KanbanCard 엔티티
     * @return 업데이트 성공 여부
     */
    suspend fun update(kanbanCard: KanbanCard): Boolean
    
    /**
     * 특정 컬럼 내에서 특정 위치 이상의 카드들의 위치를 조정합니다.
     * 
     * 비즈니스 규칙: 
     * - 칸반 카드 이동 시 다른 카드들의 위치를 시프트합니다.
     * - position >= fromPosition인 카드들을 shift만큼 이동시킵니다.
     * 
     * @param columnId 대상 컬럼 ID
     * @param fromPosition 시작 위치 (이 위치 이상의 카드들을 이동)
     * @param shift 이동할 위치 변화량 (양수: 뒤로, 음수: 앞으로)
     * @return 업데이트된 카드 수
     */
    suspend fun shiftPositions(columnId: String, fromPosition: Int, shift: Int): Int
    
    /**
     * KanbanCard를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙: deletedAt을 현재 시각으로 설정합니다.
     * 
     * @param id 삭제할 KanbanCard의 ID
     * @return 삭제 성공 여부
     */
    suspend fun softDelete(id: UUID): Boolean
    
    /**
     * Todo ID로 연결된 KanbanCard를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙: Todo 삭제 시 연결된 KanbanCard도 함께 삭제할 때 사용합니다.
     * 
     * @param todoId 삭제할 Todo의 ID
     * @return 삭제 성공 여부
     */
    suspend fun softDeleteByTodoId(todoId: UUID): Boolean
    
    /**
     * 소프트 삭제된 KanbanCard를 복구합니다.
     * 
     * 비즈니스 규칙: deletedAt을 NULL로 설정합니다.
     * 
     * @param id 복구할 KanbanCard의 ID
     * @return 복구 성공 여부
     */
    suspend fun restore(id: UUID): Boolean
    
    /**
     * Todo ID로 연결된 KanbanCard를 복구합니다.
     * 
     * 비즈니스 규칙: Todo 복구 시 연결된 KanbanCard도 함께 복구할 때 사용합니다.
     * 
     * @param todoId 복구할 Todo의 ID
     * @return 복구 성공 여부
     */
    suspend fun restoreByTodoId(todoId: UUID): Boolean
    
    /**
     * Todo ID에 해당하는 KanbanCard가 존재하는지 확인합니다.
     * 
     * 비즈니스 규칙: 활성 상태(deletedAt IS NULL)인 레코드만 확인합니다.
     * 
     * @param todoId 확인할 Todo의 ID
     * @return 존재 여부
     */
    suspend fun existsByTodoId(todoId: UUID): Boolean
}
