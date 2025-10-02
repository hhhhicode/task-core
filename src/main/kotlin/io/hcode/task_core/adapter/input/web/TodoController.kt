package io.hcode.task_core.adapter.input.web

import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.TodoResponse
import io.hcode.task_core.adapter.input.web.dto.todo.UpdateTodoRequest
import io.hcode.task_core.domain.port.input.TodoCommandPort
import io.hcode.task_core.domain.port.input.TodoQueryPort
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Todo REST Controller (Inbound Adapter)
 *
 * 비즈니스 의미: Todo 작업 관리를 위한 HTTP API를 제공합니다.
 *
 * 기술적 배경:
 * - Spring WebFlux 기반의 반응형(Reactive) REST API
 * - Kotlin Coroutines를 활용한 비동기 처리
 * - DTO 검증을 통한 입력 데이터 안정성 확보
 */
@RestController
@RequestMapping("/api/v1/todos")
class TodoController(
    private val todoCommandPort: TodoCommandPort,
    private val todoQueryPort: TodoQueryPort
) {

    /**
     * 새로운 Todo 생성
     *
     * 비즈니스 의미: 사용자가 새로운 작업을 등록합니다.
     *
     * @param request Todo 생성 요청 DTO (제목, 설명, 상태, 우선순위)
     * @return 생성된 Todo의 상세 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createTodo(
        @Valid @RequestBody request: CreateTodoRequest
    ): TodoResponse {
        val todo = todoCommandPort.createTodo(request)
        return TodoResponse.from(todo)
    }

    /**
     * ID로 Todo 조회
     *
     * 비즈니스 의미: 특정 작업의 상세 정보를 조회합니다.
     *
     * @param id Todo의 고유 식별자
     * @param includeDeleted 삭제된 Todo 포함 여부 (기본값: false)
     * @return Todo의 상세 정보
     */
    @GetMapping("/{id}")
    suspend fun getTodoById(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): TodoResponse {
        val todo = todoQueryPort.getTodoById(id, includeDeleted)
            ?: throw NoSuchElementException("Todo not found: $id")
        return TodoResponse.from(todo)
    }

    /**
     * 모든 Todo 조회
     *
     * 비즈니스 의미: 등록된 모든 작업 목록을 조회합니다.
     *
     * @param includeDeleted 삭제된 Todo 포함 여부 (기본값: false)
     * @return Todo 목록 스트림
     */
    @GetMapping
    fun getAllTodos(
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): Flow<TodoResponse> {
        return todoQueryPort.getAllTodos(includeDeleted)
            .map { TodoResponse.from(it) }
    }

    /**
     * Todo 수정
     *
     * 비즈니스 의미: 기존 작업의 정보를 변경합니다.
     *
     * @param id 수정할 Todo의 ID
     * @param request Todo 수정 요청 DTO (제목, 설명, 상태, 우선순위)
     * @return 수정된 Todo의 상세 정보
     */
    @PutMapping("/{id}")
    suspend fun updateTodo(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTodoRequest
    ): TodoResponse {
        val todo = todoCommandPort.updateTodo(id, request)
        return TodoResponse.from(todo)
    }

    /**
     * Todo 삭제 (소프트 삭제)
     *
     * 비즈니스 의미: 작업을 삭제 상태로 변경합니다. 실제 데이터는 보존됩니다.
     *
     * @param id 삭제할 Todo의 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteTodo(@PathVariable id: UUID) {
        todoCommandPort.deleteTodo(id)
    }

    /**
     * Todo 복원
     *
     * 비즈니스 의미: 삭제된 작업을 다시 활성화합니다.
     *
     * @param id 복원할 Todo의 ID
     * @return 복원된 Todo의 상세 정보
     */
    @PostMapping("/{id}/restore")
    suspend fun restoreTodo(@PathVariable id: UUID): TodoResponse {
        val todo = todoCommandPort.restoreTodo(id)
        return TodoResponse.from(todo)
    }
}
