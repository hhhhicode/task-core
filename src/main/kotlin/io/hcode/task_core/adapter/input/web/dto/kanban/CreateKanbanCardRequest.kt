package io.hcode.task_core.adapter.input.web.dto.kanban

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 칸반 카드 생성 요청 DTO
 * 
 * 비즈니스 의미: Todo를 칸반 보드에 추가하기 위한 요청 데이터를 전달합니다.
 */
data class CreateKanbanCardRequest(
    /**
     * 연결할 Todo ID (필수)
     * 비즈니스 의미: 칸반 보드에 표시할 Todo를 지정합니다.
     */
    @field:NotNull(message = "Todo ID는 필수 입력 항목입니다.")
    val todoId: UUID?,
    
    /**
     * 칸반 컬럼 ID (필수, 최대 255자, 빈 문자열 금지)
     * 비즈니스 의미: 카드가 속할 워크플로우 단계를 지정합니다.
     */
    @field:NotBlank(message = "컬럼 ID는 필수 입력 항목입니다.")
    @field:Size(max = 255, message = "컬럼 ID는 최대 255자까지 입력 가능합니다.")
    val columnId: String?,
    
    /**
     * 컬럼 내 위치 (필수, 0 이상)
     * 비즈니스 의미: 같은 컬럼 내에서 카드의 상대적 순서를 지정합니다.
     */
    @field:NotNull(message = "위치는 필수 입력 항목입니다.")
    @field:Min(0, message = "위치는 0 이상이어야 합니다.")
    val position: Int?
)
