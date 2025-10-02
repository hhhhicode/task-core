package io.hcode.task_core.domain.port.input

import io.hcode.task_core.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Todo 쿼리 포트 (Inbound Port)
 * 
 * 비즈니스 의미: Todo 엔티티의 조회를 담당하는 유스케이스입니다.
 * 헥사고날 아키텍처: 애플리케이션의 진입점으로, 외부(웹 어댑터)에서 도메인 로직을 호출합니다.
 * 
 * CQRS: Query(조회) 전용 포트로, 상태 변경 없이 데이터 조회만 담당합니다.
 */
interface TodoQueryPort {
    
    /**
     * ID로 Todo를 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 기본적으로 활성 상태(deletedAt IS NULL)의 Todo만 조회합니다.
     * - includeDeleted=true인 경우 삭제된 Todo도 조회합니다.
     * 
     * @param id 조회할 Todo의 ID
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return Todo 엔티티, 존재하지 않으면 null
     */
    suspend fun getTodoById(id: UUID, includeDeleted: Boolean = false): Todo?
    
    /**
     * 모든 Todo를 조회합니다.
     * 
     * 비즈니스 규칙:
     * - 기본적으로 활성 상태(deletedAt IS NULL)의 Todo만 조회합니다.
     * - includeDeleted=true인 경우 삭제된 Todo도 조회합니다.
     * - createdAt 내림차순으로 정렬됩니다.
     * 
     * @param includeDeleted 삭제된 레코드 포함 여부
     * @return Todo 엔티티의 Flow
     */
    fun getAllTodos(includeDeleted: Boolean = false): Flow<Todo>
}
