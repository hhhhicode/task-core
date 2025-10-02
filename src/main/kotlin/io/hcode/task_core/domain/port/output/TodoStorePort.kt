package io.hcode.task_core.domain.port.output

import io.hcode.task_core.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Todo 영속성 포트 (Outbound Port)
 * 
 * 비즈니스 의미: Todo 엔티티의 영속성 관리를 담당하는 외부 어댑터의 계약을 정의합니다.
 * 헥사고날 아키텍처: 도메인 계층이 인프라 계층에 의존하지 않도록 추상화를 제공합니다.
 * 
 * 구현: jOOQ 기반 어댑터가 이 인터페이스를 구현합니다.
 */
interface TodoStorePort {
    
    /**
     * 새로운 Todo를 생성합니다.
     * 
     * 비즈니스 규칙: 
     * - UUID는 애플리케이션 레이어에서 생성됩니다.
     * - createdAt, updatedAt은 현재 시각으로 설정됩니다.
     * 
     * @param todo 생성할 Todo 엔티티
     * @return 생성된 Todo의 ID
     */
    suspend fun create(todo: Todo): UUID
    
    /**
     * ID로 활성 상태의 Todo를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @param id 조회할 Todo의 ID
     * @return Todo 엔티티, 존재하지 않거나 삭제된 경우 null
     */
    suspend fun findByIdActive(id: UUID): Todo?
    
    /**
     * ID로 Todo를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * 비즈니스 규칙: includeDeleted 파라미터로 삭제된 레코드 포함 여부를 결정합니다.
     * 
     * @param id 조회할 Todo의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return Todo 엔티티, 존재하지 않으면 null
     */
    suspend fun findById(id: UUID, includeDeleted: Boolean = false): Todo?
    
    /**
     * 모든 활성 상태의 Todo를 조회합니다.
     * 
     * 비즈니스 규칙: deletedAt IS NULL인 레코드만 조회합니다.
     * 
     * @return Todo 엔티티의 Flow
     */
    fun findAllActive(): Flow<Todo>
    
    /**
     * 모든 Todo를 조회합니다 (삭제된 레코드 포함 가능).
     * 
     * 비즈니스 규칙: includeDeleted 파라미터로 삭제된 레코드 포함 여부를 결정합니다.
     * 
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return Todo 엔티티의 Flow
     */
    fun findAll(includeDeleted: Boolean = false): Flow<Todo>
    
    /**
     * Todo를 업데이트합니다.
     * 
     * 비즈니스 규칙: 
     * - updatedAt은 현재 시각으로 갱신됩니다.
     * - ID는 변경할 수 없습니다.
     * 
     * @param todo 업데이트할 Todo 엔티티
     * @return 업데이트 성공 여부
     */
    suspend fun update(todo: Todo): Boolean
    
    /**
     * Todo를 소프트 삭제합니다.
     * 
     * 비즈니스 규칙: 
     * - deletedAt을 현재 시각으로 설정합니다.
     * - 실제 레코드는 삭제하지 않습니다.
     * - 연결된 GanttTask, KanbanCard도 함께 소프트 삭제됩니다 (서비스 레이어에서 처리).
     * 
     * @param id 삭제할 Todo의 ID
     * @return 삭제 성공 여부
     */
    suspend fun softDelete(id: UUID): Boolean
    
    /**
     * 소프트 삭제된 Todo를 복구합니다.
     * 
     * 비즈니스 규칙: 
     * - deletedAt을 NULL로 설정합니다.
     * - updatedAt은 현재 시각으로 갱신됩니다.
     * 
     * @param id 복구할 Todo의 ID
     * @return 복구 성공 여부
     */
    suspend fun restore(id: UUID): Boolean
    
    /**
     * Todo가 존재하는지 확인합니다.
     * 
     * 비즈니스 규칙: 활성 상태(deletedAt IS NULL)인 레코드만 확인합니다.
     * 
     * @param id 확인할 Todo의 ID
     * @return 존재 여부
     */
    suspend fun existsById(id: UUID): Boolean
}
