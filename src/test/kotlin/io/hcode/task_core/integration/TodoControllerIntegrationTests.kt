package io.hcode.task_core.integration

import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.TodoResponse
import io.hcode.task_core.adapter.input.web.dto.todo.UpdateTodoRequest
import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.UUID

/**
 * TodoController 통합 테스트
 *
 * 비즈니스 의미: Todo CRUD 및 복원 기능의 전체 통합 흐름을 검증합니다.
 *
 * 테스트 범위:
 * - HTTP 요청/응답
 * - DTO 검증
 * - 서비스 계층
 * - 데이터베이스 영속성
 * - 에러 처리
 */
class TodoControllerIntegrationTests : IntegrationTestBase() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `Todo 생성 성공`() {
        // Given
        val request = CreateTodoRequest(
            title = "통합 테스트 Todo",
            description = "Testcontainers로 생성된 Todo",
            status = TaskStatus.TODO,
            priority = TaskPriority.HIGH
        )

        // When & Then
        webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .consumeWith { response ->
                val todo = response.responseBody!!
                assert(todo.title == request.title)
                assert(todo.description == request.description)
                assert(todo.status == request.status)
                assert(todo.priority == request.priority)
                assert(todo.deletedAt == null)
            }
    }

    @Test
    fun `Todo 생성 시 필수 필드 누락하면 400 에러`() {
        // Given - title이 빈 문자열
        val invalidRequest = mapOf(
            "title" to "",
            "description" to "설명",
            "status" to "TODO",
            "priority" to "LOW"
        )

        // When & Then
        webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `Todo 조회 성공`() {
        // Given - Todo 먼저 생성
        val createRequest = CreateTodoRequest(
            title = "조회할 Todo",
            description = "조회 테스트",
            status = TaskStatus.TODO,
            priority = TaskPriority.MEDIUM
        )

        val createdTodo = webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .returnResult()
            .responseBody!!

        // When & Then
        webTestClient.get()
            .uri("/api/v1/todos/${createdTodo.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody<TodoResponse>()
            .consumeWith { response ->
                val todo = response.responseBody!!
                assert(todo.id == createdTodo.id)
                assert(todo.title == createRequest.title)
            }
    }

    @Test
    fun `존재하지 않는 Todo 조회 시 404 에러`() {
        // Given
        val nonExistentId = UUID.randomUUID()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/todos/$nonExistentId")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Todo 수정 성공`() {
        // Given - Todo 먼저 생성
        val createRequest = CreateTodoRequest(
            title = "수정 전 제목",
            description = "수정 전 설명",
            status = TaskStatus.TODO,
            priority = TaskPriority.LOW
        )

        val createdTodo = webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .returnResult()
            .responseBody!!

        // When - 수정
        val updateRequest = UpdateTodoRequest(
            title = "수정 후 제목",
            description = "수정 후 설명",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH
        )

        // Then
        webTestClient.put()
            .uri("/api/v1/todos/${createdTodo.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<TodoResponse>()
            .consumeWith { response ->
                val todo = response.responseBody!!
                assert(todo.title == updateRequest.title)
                assert(todo.description == updateRequest.description)
                assert(todo.status == updateRequest.status)
                assert(todo.priority == updateRequest.priority)
            }
    }

    @Test
    fun `Todo 삭제 성공`() {
        // Given - Todo 먼저 생성
        val createRequest = CreateTodoRequest(
            title = "삭제할 Todo",
            description = "삭제 테스트",
            status = TaskStatus.TODO,
            priority = TaskPriority.LOW
        )

        val createdTodo = webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .returnResult()
            .responseBody!!

        // When - 삭제
        webTestClient.delete()
            .uri("/api/v1/todos/${createdTodo.id}")
            .exchange()
            .expectStatus().isNoContent

        // Then - 삭제된 Todo는 기본 조회에서 안 보임
        webTestClient.get()
            .uri("/api/v1/todos/${createdTodo.id}")
            .exchange()
            .expectStatus().isNotFound

        // But - includeDeleted=true로 조회 가능
        webTestClient.get()
            .uri("/api/v1/todos/${createdTodo.id}?includeDeleted=true")
            .exchange()
            .expectStatus().isOk
            .expectBody<TodoResponse>()
            .consumeWith { response ->
                val todo = response.responseBody!!
                assert(todo.deletedAt != null)
            }
    }

    @Test
    fun `Todo 복원 성공`() {
        // Given - Todo 생성 후 삭제
        val createRequest = CreateTodoRequest(
            title = "복원할 Todo",
            description = "복원 테스트",
            status = TaskStatus.TODO,
            priority = TaskPriority.MEDIUM
        )

        val createdTodo = webTestClient.post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .returnResult()
            .responseBody!!

        webTestClient.delete()
            .uri("/api/v1/todos/${createdTodo.id}")
            .exchange()
            .expectStatus().isNoContent

        // When - 복원
        webTestClient.post()
            .uri("/api/v1/todos/${createdTodo.id}/restore")
            .exchange()
            .expectStatus().isOk
            .expectBody<TodoResponse>()
            .consumeWith { response ->
                val todo = response.responseBody!!
                assert(todo.deletedAt == null)
                assert(todo.title == createRequest.title)
            }

        // Then - 기본 조회에서 다시 보임
        webTestClient.get()
            .uri("/api/v1/todos/${createdTodo.id}")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `모든 Todo 목록 조회 성공`() {
        // Given - 여러 Todo 생성
        val requests = listOf(
            CreateTodoRequest("Todo 1", "설명 1", TaskStatus.TODO, TaskPriority.LOW),
            CreateTodoRequest("Todo 2", "설명 2", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM),
            CreateTodoRequest("Todo 3", "설명 3", TaskStatus.DONE, TaskPriority.HIGH)
        )

        requests.forEach { request ->
            webTestClient.post()
                .uri("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
        }

        // When & Then
        val result = webTestClient.get()
            .uri("/api/v1/todos")
            .exchange()
            .expectStatus().isOk
            .returnResult(TodoResponse::class.java)
            .responseBody
            .collectList()
            .block()!!

        // 최소한 생성한 3개는 있어야 함 (이전 테스트의 데이터가 남아있을 수 있음)
        assert(result.size >= 3)
    }
}
