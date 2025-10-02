package io.hcode.task_core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * 칸반 보드 상의 Todo 표현을 나타내는 도메인 엔티티
 * 
 * 비즈니스 의미: Todo를 칸반 보드의 컬럼 기반 뷰로 표현합니다.
 * 컬럼 ID와 위치를 통해 시각적 워크플로우 관리를 지원합니다.
 * 
 * 불변성: 이 엔티티는 data class로 불변성을 유지하며, 변경 시 새로운 인스턴스를 생성합니다.
 */
data class KanbanCard(
    /**
     * 고유 식별자
     * 비즈니스 의미: 칸반 보드 상의 카드를 유일하게 식별합니다.
     */
    val id: UUID,
    
    /**
     * 연결된 Todo ID (Unique FK)
     * 비즈니스 의미: 하나의 Todo는 최대 하나의 KanbanCard를 가질 수 있습니다.
     */
    val todoId: UUID,
    
    /**
     * 칸반 컬럼 ID (최대 255자, 빈 문자열 금지)
     * 비즈니스 의미: 카드가 속한 워크플로우 단계를 나타냅니다.
     * 예: "backlog", "in-progress", "review", "done"
     */
    val columnId: String,
    
    /**
     * 컬럼 내 위치 (0 이상)
     * 비즈니스 의미: 같은 컬럼 내에서 카드의 상대적 순서를 나타냅니다.
     * 낮은 숫자가 상위에 위치하며, 0부터 시작합니다.
     */
    val position: Int,
    
    /**
     * 소프트 삭제 시각 (NULL = 활성)
     * 비즈니스 의미: 칸반 카드가 삭제된 시점을 기록하며, NULL이면 활성 상태입니다.
     */
    val deletedAt: Instant? = null
) {
    init {
        require(columnId.isNotBlank()) {
            "컬럼 ID는 빈 문자열일 수 없습니다."
        }
        require(columnId.length <= 255) {
            "컬럼 ID는 최대 255자까지 허용됩니다. 현재 길이: ${columnId.length}"
        }
        require(position >= 0) {
            "위치는 0 이상이어야 합니다. 현재 값: $position"
        }
    }
    
    /**
     * 칸반 카드가 활성 상태인지 확인합니다.
     * 비즈니스 규칙: deletedAt이 NULL이면 활성 상태입니다.
     */
    fun isActive(): Boolean = deletedAt == null
    
    /**
     * 칸반 카드를 소프트 삭제합니다.
     * 비즈니스 규칙: 실제 데이터를 삭제하지 않고 deletedAt 타임스탬프를 설정합니다.
     */
    fun softDelete(deletedAt: Instant): KanbanCard =
        copy(deletedAt = deletedAt)
    
    /**
     * 소프트 삭제된 칸반 카드를 복구합니다.
     * 비즈니스 규칙: deletedAt을 NULL로 설정하여 카드를 다시 활성화합니다.
     */
    fun restore(): KanbanCard =
        copy(deletedAt = null)
    
    /**
     * 칸반 카드를 다른 컬럼으로 이동하거나 같은 컬럼 내에서 위치를 변경합니다.
     * 비즈니스 규칙: 컬럼 ID나 위치를 변경하며, 유효성 검증을 수행합니다.
     */
    fun moveTo(newColumnId: String, newPosition: Int): KanbanCard =
        copy(columnId = newColumnId, position = newPosition)
    
    /**
     * 같은 컬럼 내에서 위치만 변경합니다.
     * 비즈니스 규칙: 컬럼 ID는 유지하고 위치만 업데이트합니다.
     */
    fun updatePosition(newPosition: Int): KanbanCard =
        copy(position = newPosition)
    
    /**
     * 다른 컬럼으로 이동합니다.
     * 비즈니스 규칙: 위치는 유지하고 컬럼 ID만 변경합니다.
     */
    fun moveToColumn(newColumnId: String): KanbanCard =
        copy(columnId = newColumnId)
}
