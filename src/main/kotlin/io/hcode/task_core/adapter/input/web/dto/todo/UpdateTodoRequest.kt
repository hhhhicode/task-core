package io.hcode.task_core.adapter.input.web.dto.todo

import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import jakarta.validation.constraints.Size

/**
 * Todo 수정 요청 DTO
 * 
 * 비즈니스 의미: 기존 Todo 작업의 정보를 부분적으로 업데이트하기 위한 요청 데이터를 전달합니다.
 * 모든 필드는 선택사항이며, 제공된 필드만 업데이트됩니다.
 */
data class UpdateTodoRequest(
    /**
     * 제목 (선택사항, 최대 255자)
     * 비즈니스 의미: 작업의 제목을 변경합니다.
     */
    @field:Size(min = 1, max = 255, message = "제목은 1자 이상 255자 이하여야 합니다.")
    val title: String? = null,
    
    /**
     * 설명 (선택사항, 최대 10000자)
     * 비즈니스 의미: 작업의 설명을 변경합니다.
     */
    @field:Size(max = 10000, message = "설명은 최대 10000자까지 입력 가능합니다.")
    val description: String? = null,
    
    /**
     * 상태 (선택사항)
     * 비즈니스 의미: 작업의 진행 단계를 변경합니다.
     */
    val status: TaskStatus? = null,
    
    /**
     * 우선순위 (선택사항)
     * 비즈니스 의미: 작업의 중요도를 변경합니다.
     */
    val priority: TaskPriority? = null
)
