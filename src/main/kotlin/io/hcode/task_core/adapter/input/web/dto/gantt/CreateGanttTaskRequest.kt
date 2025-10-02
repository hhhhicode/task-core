package io.hcode.task_core.adapter.input.web.dto.gantt

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/**
 * 간트 태스크 생성 요청 DTO
 * 
 * 비즈니스 의미: Todo를 간트 차트에 추가하기 위한 요청 데이터를 전달합니다.
 */
data class CreateGanttTaskRequest(
    /**
     * 연결할 Todo ID (필수)
     * 비즈니스 의미: 간트 차트에 표시할 Todo를 지정합니다.
     */
    @field:NotNull(message = "Todo ID는 필수 입력 항목입니다.")
    val todoId: UUID?,
    
    /**
     * 시작 일시 (필수)
     * 비즈니스 의미: 작업의 계획된 시작 시점을 나타냅니다.
     */
    @field:NotNull(message = "시작 일시는 필수 입력 항목입니다.")
    val startDate: Instant?,
    
    /**
     * 종료 일시 (필수)
     * 비즈니스 의미: 작업의 계획된 완료 시점을 나타냅니다.
     */
    @field:NotNull(message = "종료 일시는 필수 입력 항목입니다.")
    val endDate: Instant?,
    
    /**
     * 진행률 (기본값: 0, 0-100)
     * 비즈니스 의미: 작업의 현재 진행 정도를 백분율로 표현합니다.
     */
    @field:Min(0, message = "진행률은 0 이상이어야 합니다.")
    @field:Max(100, message = "진행률은 100 이하여야 합니다.")
    val progress: Int = 0
) {
    /**
     * 시작일이 종료일보다 이전이거나 같은지 검증합니다.
     * 비즈니스 규칙: 시작일은 종료일보다 이후일 수 없습니다.
     */
    @AssertTrue(message = "시작일은 종료일보다 이전이거나 같아야 합니다.")
    fun isChronological(): Boolean {
        if (startDate == null || endDate == null) return true
        return !startDate.isAfter(endDate)
    }
}
