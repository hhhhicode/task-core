package io.hcode.task_core.adapter.input.web.dto.todo

import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Todo 생성 요청 DTO
 *
 * 비즈니스 의미: 새로운 Todo 작업을 생성하기 위한 요청 데이터를 전달합니다.
 */
data class CreateTodoRequest(
    /**
     * 제목 (필수, 최대 255자, 빈 문자열 금지)
     * 비즈니스 의미: 작업의 핵심 내용을 간결하게 표현합니다.
     */
    @field:NotBlank(message = "제목은 필수 입력 항목입니다.")
    @field:Size(max = 255, message = "제목은 최대 255자까지 입력 가능합니다.")
    val title: String,

    /**
     * 설명 (선택사항, 최대 10000자)
     * 비즈니스 의미: 작업에 대한 상세한 설명이나 컨텍스트를 제공합니다.
     */
    @field:Size(max = 10000, message = "설명은 최대 10000자까지 입력 가능합니다.")
    val description: String? = null,

    /**
     * 상태 (기본값: TODO)
     * 비즈니스 의미: 작업의 초기 진행 단계를 지정합니다.
     */
    @field:NotNull(message = "상태는 필수 입력 항목입니다.")
    val status: TaskStatus? = TaskStatus.TODO,

    /**
     * 우선순위 (기본값: MEDIUM)
     * 비즈니스 의미: 작업의 중요도와 긴급도를 표현합니다.
     */
    val priority: TaskPriority = TaskPriority.MEDIUM
)
