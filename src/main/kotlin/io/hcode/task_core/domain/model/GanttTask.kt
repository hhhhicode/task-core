package io.hcode.task_core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * 간트 차트 상의 Todo 표현을 나타내는 도메인 엔티티
 * 
 * 비즈니스 의미: Todo를 간트 차트의 타임라인 뷰로 표현합니다.
 * 시작일, 종료일, 진행률을 통해 프로젝트 일정 관리를 지원합니다.
 * 
 * 불변성: 이 엔티티는 data class로 불변성을 유지하며, 변경 시 새로운 인스턴스를 생성합니다.
 */
data class GanttTask(
    /**
     * 고유 식별자
     * 비즈니스 의미: 간트 차트 상의 태스크를 유일하게 식별합니다.
     */
    val id: UUID,
    
    /**
     * 연결된 Todo ID (Unique FK)
     * 비즈니스 의미: 하나의 Todo는 최대 하나의 GanttTask를 가질 수 있습니다.
     */
    val todoId: UUID,
    
    /**
     * 시작 일시 (UTC)
     * 비즈니스 의미: 작업의 계획된 시작 시점을 나타냅니다.
     */
    val startDate: Instant,
    
    /**
     * 종료 일시 (UTC)
     * 비즈니스 의미: 작업의 계획된 완료 시점을 나타냅니다.
     * 비즈니스 규칙: startDate <= endDate 이어야 합니다.
     */
    val endDate: Instant,
    
    /**
     * 진행률 (0-100)
     * 비즈니스 의미: 작업의 현재 진행 정도를 백분율로 표현합니다.
     * 비즈니스 규칙: 0 이상 100 이하의 값이어야 합니다.
     */
    val progress: Int = 0,
    
    /**
     * 소프트 삭제 시각 (NULL = 활성)
     * 비즈니스 의미: 간트 태스크가 삭제된 시점을 기록하며, NULL이면 활성 상태입니다.
     */
    val deletedAt: Instant? = null
) {
    init {
        require(progress in 0..100) {
            "진행률은 0 이상 100 이하여야 합니다. 현재 값: $progress"
        }
        require(!startDate.isAfter(endDate)) {
            "시작일은 종료일보다 이전이거나 같아야 합니다. startDate: $startDate, endDate: $endDate"
        }
    }
    
    /**
     * 간트 태스크가 활성 상태인지 확인합니다.
     * 비즈니스 규칙: deletedAt이 NULL이면 활성 상태입니다.
     */
    fun isActive(): Boolean = deletedAt == null
    
    /**
     * 간트 태스크를 소프트 삭제합니다.
     * 비즈니스 규칙: 실제 데이터를 삭제하지 않고 deletedAt 타임스탬프를 설정합니다.
     */
    fun softDelete(deletedAt: Instant): GanttTask =
        copy(deletedAt = deletedAt)
    
    /**
     * 소프트 삭제된 간트 태스크를 복구합니다.
     * 비즈니스 규칙: deletedAt을 NULL로 설정하여 태스크를 다시 활성화합니다.
     */
    fun restore(): GanttTask =
        copy(deletedAt = null)
    
    /**
     * 간트 태스크 정보를 업데이트합니다.
     * 비즈니스 규칙: 날짜와 진행률을 업데이트할 수 있으며, 불변성 검증을 수행합니다.
     */
    fun update(
        startDate: Instant? = null,
        endDate: Instant? = null,
        progress: Int? = null
    ): GanttTask {
        val newStartDate = startDate ?: this.startDate
        val newEndDate = endDate ?: this.endDate
        val newProgress = progress ?: this.progress
        
        return copy(
            startDate = newStartDate,
            endDate = newEndDate,
            progress = newProgress
        )
    }
    
    /**
     * 작업 기간(일수)을 계산합니다.
     * 비즈니스 의미: 계획된 작업 기간을 일 단위로 반환합니다.
     */
    fun getDurationInDays(): Long {
        return java.time.Duration.between(startDate, endDate).toDays()
    }
    
    /**
     * 작업이 완료되었는지 확인합니다.
     * 비즈니스 규칙: 진행률이 100%이면 완료된 것으로 간주합니다.
     */
    fun isCompleted(): Boolean = progress == 100
}
