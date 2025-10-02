package io.hcode.task_core.domain.port.output

import io.hcode.task_core.domain.model.GanttTask
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * GanttTask 영속성 포트 (Outbound Port)
 * 
 * 비즈니스 의미: GanttTask 엔티티의 영속성 관리를 담당하는 외부 어댑터의 계약을 정의합니다.
 * 헥사고날 아키텍처: 도메인 계층이 인프라 계층에 의존하지 않도록 추상화를 제공합니다.
 * 
 * 구현: jOOQ 기반 어댑터가 이 인터페이스를 구현합니다.
 */
interface GanttStorePort {
    
    /**
     * 새로운 GanttTask를 생성합니다.
     * 
     * 비즈니스 규칙:
     * - todoId는 유일해야 합니다 (하나의 Todo는 최대 하나의 GanttTask).
     * - UUID는 애플리케이션 레이어에서 생성됩니다.
     * 
     * @param ganttTask 생성할 GanttTask 엔티티
     * @return 생성된 GanttTask의 ID
     */
    suspend fun create(ganttTask: GanttTask): UUID
    
    /**
     * ID로 활성 상태의 GanttTask를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param id 조회할 GanttTask의 ID
     * @return GanttTask 엔티티, 존재하지 않거나 삭제된 경우 null
     */
    suspend fun findByIdActive(id: UUID): GanttTask?
    
    /**
     * ID로 GanttTask를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * @param id 조회할 GanttTask의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return GanttTask 엔티티, 존재하지 않으면 null
     */
    suspend fun findById(id: UUID, includeDeleted: Boolean = false): GanttTask?
    
    /**
     * Todo ID로 활성 상태의 GanttTask를 조회합니다.
     * 
     * 비즈니스 규칙: 
     * - 하나의 Todo는 최대 하나의 GanttTask를 가집니다.
     * - deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param todoId 조회할 Todo의 ID
     * @return GanttTask 엔티티, 존재하지 않거나 삭제된 경우 null
     */
    suspend fun findByTodoIdActive(todoId: UUID): GanttTask?
    
    /**
     * 모든 활성 상태의 GanttTask를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @return GanttTask 엔티티의 Flow
     */
    fun findAllActive(): Flow<GanttTask>
    
    /**
     * 모든 GanttTask를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return GanttTask 엔티티의 Flow
     */
    fun findAll(includeDeleted: Boolean = false): Flow<GanttTask>
    
    /**
     * 여러 Todo ID에 해당하는 활성 GanttTask들을 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param todoIds 조회할 Todo ID 목록
     * @return GanttTask 엔티티의 Flow
     */
    fun findByTodoIdsActive(todoIds: Collection<UUID>): Flow<GanttTask>
    
    /**
     * GanttTask를 업데이트합니다.
     * 
     * 비즈니스 규칙: ID와 todoId는 변경할 수 없습니다.
     * 
     * @param ganttTask 업데이트할 GanttTask 엔티티
     * @return 업데이트 성공 여부
     */
    suspend fun update(ganttTask: GanttTask): Boolean
    
    /**
     * GanttTask를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙: deletedAt을 현재 시각으로 설정합니다.
     * 
     * @param id 삭제할 GanttTask의 ID
     * @return 삭제 성공 여부
     */
    suspend fun softDelete(id: UUID): Boolean
    
    /**
     * Todo ID로 연결된 GanttTask를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙: Todo 삭제 시 연결된 GanttTask도 함께 삭제할 때 사용합니다.
     * 
     * @param todoId 삭제할 Todo의 ID
     * @return 삭제 성공 여부
     */
    suspend fun softDeleteByTodoId(todoId: UUID): Boolean
    
    /**
     * 소프트 삭제된 GanttTask를 복구합니다.
     * 
     * 비즈니스 규칙: deletedAt을 NULL로 설정합니다.
     * 
     * @param id 복구할 GanttTask의 ID
     * @return 복구 성공 여부
     */
    suspend fun restore(id: UUID): Boolean
    
    /**
     * Todo ID로 연결된 GanttTask를 복구합니다.
     * 
     * 비즈니스 규칙: Todo 복구 시 연결된 GanttTask도 함께 복구할 때 사용합니다.
     * 
     * @param todoId 복구할 Todo의 ID
     * @return 복구 성공 여부
     */
    suspend fun restoreByTodoId(todoId: UUID): Boolean
    
    /**
     * Todo ID에 해당하는 GanttTask가 존재하는지 확인합니다.
     * 
     * 비즈니스 규칙: 활성 상태(deletedAt IS NULL)인 레코드만 확인합니다.
     * 
     * @param todoId 확인할 Todo의 ID
     * @return 존재 여부
     */
    suspend fun existsByTodoId(todoId: UUID): Boolean
}
