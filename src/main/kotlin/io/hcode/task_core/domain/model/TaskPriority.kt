package io.hcode.task_core.domain.model

/**
 * Todo 작업의 우선순위를 나타내는 열거형
 * 
 * 비즈니스 의미: 작업의 중요도와 긴급도를 표현하며, 작업 우선순위 결정에 활용됩니다.
 */
enum class TaskPriority {
    /**
     * 낮음 - 긴급하지 않고 중요도가 낮은 작업
     */
    LOW,
    
    /**
     * 보통 - 일반적인 우선순위의 작업 (기본값)
     */
    MEDIUM,
    
    /**
     * 높음 - 긴급하거나 중요도가 높은 작업
     */
    HIGH
}
