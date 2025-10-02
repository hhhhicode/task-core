package io.hcode.task_core.adapter.input.web.dto.kanban

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 칸반 카드 이동 요청 DTO
 * 
 * 비즈니스 의미: 칸반 카드를 다른 컬럼으로 이동하거나 같은 컬럼 내에서 위치를 변경하기 위한 요청 데이터를 전달합니다.
 */
data class MoveKanbanCardRequest(
    /**
     * 새 칸반 컬럼 ID (필수, 최대 255자, 빈 문자열 금지)
     * 비즈니스 의미: 카드를 이동할 목적지 컬럼을 지정합니다.
     */
    @field:NotBlank(message = "컬럼 ID는 필수 입력 항목입니다.")
    @field:Size(max = 255, message = "컬럼 ID는 최대 255자까지 입력 가능합니다.")
    val newColumnId: String,
    
    /**
     * 새 위치 (필수, 0 이상)
     * 비즈니스 의미: 목적지 컬럼 내에서 카드가 위치할 순서를 지정합니다.
     */
    @field:NotNull(message = "위치는 필수 입력 항목입니다.")
    @field:Min(0, message = "위치는 0 이상이어야 합니다.")
    val newPosition: Int
)
