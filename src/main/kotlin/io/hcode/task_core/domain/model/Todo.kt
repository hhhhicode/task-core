package io.hcode.task_core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Todo 작업의 원자 단위를 표현하는 도메인 엔티티
 * 
 * 비즈니스 의미: 프로젝트 관리 시스템의 가장 기본적인 작업 단위입니다.
 * 각 Todo는 간트 차트나 칸반 보드에서 다양한 형태로 표현될 수 있습니다.
 * 
 * 불변성: 이 엔티티는 data class로 불변성을 유지하며, 변경 시 새로운 인스턴스를 생성합니다.
 */
data class Todo(
    /**
     * 고유 식별자
     * 비즈니스 의미: 시스템 전체에서 Todo를 유일하게 식별합니다.
     */
    val id: UUID,
    
    /**
     * 제목 (최대 255자, 빈 문자열 금지)
     * 비즈니스 의미: 작업의 핵심 내용을 간결하게 표현합니다.
     */
    val title: String,
    
    /**
     * 설명 (선택사항)
     * 비즈니스 의미: 작업에 대한 상세한 설명이나 컨텍스트를 제공합니다.
     */
    val description: String? = null,
    
    /**
     * 상태 (TODO, IN_PROGRESS, DONE)
     * 비즈니스 의미: 작업의 현재 진행 단계를 나타냅니다.
     */
    val status: TaskStatus,
    
    /**
     * 우선순위 (LOW, MEDIUM, HIGH)
     * 비즈니스 의미: 작업의 중요도와 긴급도를 표현합니다.
     */
    val priority: TaskPriority,
    
    /**
     * 생성 시각 (UTC)
     * 비즈니스 의미: 작업이 시스템에 등록된 시점을 기록합니다.
     */
    val createdAt: Instant,
    
    /**
     * 수정 시각 (UTC)
     * 비즈니스 의미: 작업이 마지막으로 변경된 시점을 추적합니다.
     */
    val updatedAt: Instant,
    
    /**
     * 소프트 삭제 시각 (NULL = 활성)
     * 비즈니스 의미: 작업이 삭제된 시점을 기록하며, NULL이면 활성 상태입니다.
     */
    val deletedAt: Instant? = null
) {
    /**
     * 작업이 활성 상태인지 확인합니다.
     * 비즈니스 규칙: deletedAt이 NULL이면 활성 상태입니다.
     */
    fun isActive(): Boolean = deletedAt == null
    
    /**
     * 작업을 소프트 삭제합니다.
     * 비즈니스 규칙: 실제 데이터를 삭제하지 않고 deletedAt 타임스탬프를 설정합니다.
     */
    fun softDelete(deletedAt: Instant): Todo =
        copy(deletedAt = deletedAt, updatedAt = deletedAt)
    
    /**
     * 소프트 삭제된 작업을 복구합니다.
     * 비즈니스 규칙: deletedAt을 NULL로 설정하여 작업을 다시 활성화합니다.
     */
    fun restore(restoredAt: Instant): Todo =
        copy(deletedAt = null, updatedAt = restoredAt)
    
    /**
     * 작업 정보를 업데이트합니다.
     * 비즈니스 규칙: 변경 가능한 필드만 업데이트하며, updatedAt을 갱신합니다.
     */
    fun update(
        title: String? = null,
        description: String? = null,
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        updatedAt: Instant
    ): Todo = copy(
        title = title ?: this.title,
        description = description ?: this.description,
        status = status ?: this.status,
        priority = priority ?: this.priority,
        updatedAt = updatedAt
    )
}
