package io.hcode.task_core.domain.model

/**
 * Todo 작업의 상태를 나타내는 열거형
 * 
 * 비즈니스 의미: 작업의 진행 단계를 표현하며, 워크플로우 상태 전이를 관리합니다.
 */
enum class TaskStatus {
    /**
     * 대기 중 - 아직 시작하지 않은 작업
     */
    TODO,
    
    /**
     * 진행 중 - 현재 작업이 진행되고 있는 상태
     */
    IN_PROGRESS,
    
    /**
     * 완료 - 작업이 완료된 상태
     */
    DONE
}
