package io.hcode.task_core.adapter.input.web.dto.gantt

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant

/**
 * 간트 태스크 수정 요청 DTO
 * 
 * 비즈니스 의미: 기존 간트 태스크의 기간이나 진행률을 업데이트하기 위한 요청 데이터를 전달합니다.
 * 모든 필드는 선택사항이며, 제공된 필드만 업데이트됩니다.
 */
data class UpdateGanttTaskRequest(
    /**
     * 시작 일시 (선택사항)
     * 비즈니스 의미: 작업의 계획된 시작 시점을 변경합니다.
     */
    val startDate: Instant? = null,
    
    /**
     * 종료 일시 (선택사항)
     * 비즈니스 의미: 작업의 계획된 완료 시점을 변경합니다.
     */
    val endDate: Instant? = null,
    
    /**
     * 진행률 (선택사항, 0-100)
     * 비즈니스 의미: 작업의 현재 진행 정도를 변경합니다.
     */
    @field:Min(0, message = "진행률은 0 이상이어야 합니다.")
    @field:Max(100, message = "진행률은 100 이하여야 합니다.")
    val progress: Int? = null
) {
    /**
     * 시작일이 종료일보다 이전이거나 같은지 검증합니다.
     * 비즈니스 규칙: 시작일은 종료일보다 이후일 수 없습니다.
     * 
     * 주의: 부분 업데이트 시 기존 값과의 조합도 고려해야 하므로,
     * 서비스 레이어에서 추가 검증이 필요합니다.
     */
    @AssertTrue(message = "시작일은 종료일보다 이전이거나 같아야 합니다.")
    fun isChronological(): Boolean {
        // 둘 다 제공된 경우에만 검증
        if (startDate != null && endDate != null) {
            return !startDate.isAfter(endDate)
        }
        // 하나만 제공되거나 둘 다 없는 경우 DTO 레벨에서는 통과
        // (서비스 레이어에서 기존 값과 함께 검증)
        return true
    }
}
