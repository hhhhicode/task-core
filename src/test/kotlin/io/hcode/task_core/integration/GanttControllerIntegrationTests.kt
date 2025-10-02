package io.hcode.task_core.integration

import io.hcode.task_core.adapter.input.web.dto.gantt.CreateGanttTaskRequest
import io.hcode.task_core.adapter.input.web.dto.gantt.GanttTaskResponse
import io.hcode.task_core.adapter.input.web.dto.gantt.UpdateGanttTaskRequest
import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.TodoResponse
import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Gantt Controller 통합 테스트
 *
 * 비즈니스 의미: GanttTask REST API의 완전한 CRUD 동작과 비즈니스 규칙을 검증합니다.
 *
 * 테스트 커버리지:
 * 1. Gantt Task 생성 (POST /api/v1/gantt/tasks)
 * 2. Gantt Task 조회 (GET /api/v1/gantt/tasks/{id})
 * 3. Gantt Task 필터 조회 (GET /api/v1/gantt/tasks?todoId=...)
 * 4. Gantt Task 수정 (PUT /api/v1/gantt/tasks/{id})
 * 5. Gantt Task 삭제 (DELETE /api/v1/gantt/tasks/{id})
 * 6. Gantt Task 복원 (POST /api/v1/gantt/tasks/{id}/restore)
 * 7. 유효성 검증 (startDate <= endDate, progress 0-100)
 * 8. 에러 케이스 (존재하지 않는 Todo, 중복 todoId, 404 등)
 */
class GanttControllerIntegrationTests : IntegrationTestBase() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    /**
     * 테스트 헬퍼: Todo 생성
     *
     * 비즈니스 의미: GanttTask는 Todo를 참조하므로 테스트 전 Todo를 먼저 생성합니다.
     */
    private fun createTodo(title: String = "Test Todo for Gantt"): UUID {
        return webTestClient.post()
            .uri("/api/v1/todos")
            .bodyValue(
                CreateTodoRequest(
                    title = title,
                    description = "Description for $title",
                    status = TaskStatus.TODO,
                    priority = TaskPriority.MEDIUM
                )
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody<TodoResponse>()
            .returnResult()
            .responseBody!!
            .id
    }

    @Test
    fun `Gantt Task 생성 성공`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for Gantt Creation Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val startDate = now
        val endDate = now.plus(7, ChronoUnit.DAYS)

        // When: Gantt Task 생성
        val response = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = startDate,
                    endDate = endDate,
                    progress = 0
                )
            )
            .exchange()

        // Then: 201 Created
        response.expectStatus().isCreated
            .expectBody<GanttTaskResponse>()
            .consumeWith { result ->
                val task = result.responseBody!!
                assert(task.id != UUID(0, 0))
                assert(task.todoId == todoId)
                assert(task.startDate == startDate)
                assert(task.endDate == endDate)
                assert(task.progress == 0)
            }
    }

    @Test
    fun `Gantt Task 조회 성공`() {
        // Given: Todo 및 Gantt Task 생성
        val todoId = createTodo("Todo for Gantt Retrieval Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val taskId = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS),
                    progress = 50
                )
            )
            .exchange()
            .expectBody<GanttTaskResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: 단일 Gantt Task 조회
        val response = webTestClient.get()
            .uri("/api/v1/gantt/tasks/$taskId")
            .exchange()

        // Then: 200 OK
        response.expectStatus().isOk
            .expectBody<GanttTaskResponse>()
            .consumeWith { result ->
                val task = result.responseBody!!
                assert(task.id == taskId)
                assert(task.todoId == todoId)
                assert(task.progress == 50)
            }
    }

    @Test
    fun `todoId로 Gantt Task 조회 성공`() {
        // Given: 2개의 Todo와 각각의 Gantt Task 생성
        val todoId1 = createTodo("Todo 1 for Filter Test")
        val todoId2 = createTodo("Todo 2 for Filter Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId1,
                    startDate = now,
                    endDate = now.plus(3, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectStatus().isCreated

        webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId2,
                    startDate = now,
                    endDate = now.plus(7, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectStatus().isCreated

        // When: todoId1로 필터 조회
        val response = webTestClient.get()
            .uri("/api/v1/gantt/tasks?todoId=$todoId1")
            .exchange()

        // Then: 200 OK, todoId1에 해당하는 task만 반환
        response.expectStatus().isOk

        val tasks = response.expectBodyList(GanttTaskResponse::class.java)
            .returnResult()
            .responseBody!!
        assert(tasks.size == 1)
        assert(tasks[0].todoId == todoId1)
    }

    @Test
    fun `모든 Gantt Task 조회 성공`() {
        // Given: 2개의 Gantt Task 생성
        val todoId1 = createTodo("Todo 1 for All Tasks Test")
        val todoId2 = createTodo("Todo 2 for All Tasks Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId1,
                    startDate = now,
                    endDate = now.plus(3, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectStatus().isCreated

        webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId2,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectStatus().isCreated

        // When: 전체 Gantt Task 조회
        val response = webTestClient.get()
            .uri("/api/v1/gantt/tasks")
            .exchange()

        // Then: 200 OK, 최소 2개 이상의 task 반환 (이전 테스트 데이터 포함 가능)
        response.expectStatus().isOk

        val tasks = response.expectBodyList(GanttTaskResponse::class.java)
            .returnResult()
            .responseBody!!
        assert(tasks.size >= 2)
    }

    @Test
    fun `Gantt Task 수정 성공`() {
        // Given: Gantt Task 생성
        val todoId = createTodo("Todo for Gantt Update Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val taskId = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS),
                    progress = 0
                )
            )
            .exchange()
            .expectBody<GanttTaskResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: 진행률 및 종료일 수정
        val newEndDate = now.plus(10, ChronoUnit.DAYS)
        val response = webTestClient.put()
            .uri("/api/v1/gantt/tasks/$taskId")
            .bodyValue(
                UpdateGanttTaskRequest(
                    startDate = now,
                    endDate = newEndDate,
                    progress = 75
                )
            )
            .exchange()

        // Then: 200 OK, 수정된 값 반영
        response.expectStatus().isOk
            .expectBody<GanttTaskResponse>()
            .consumeWith { result ->
                val task = result.responseBody!!
                assert(task.id == taskId)
                assert(task.endDate == newEndDate)
                assert(task.progress == 75)
            }
    }

    @Test
    fun `Gantt Task 삭제 성공`() {
        // Given: Gantt Task 생성
        val todoId = createTodo("Todo for Gantt Delete Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val taskId = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectBody<GanttTaskResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: Gantt Task 삭제 (소프트 삭제)
        webTestClient.delete()
            .uri("/api/v1/gantt/tasks/$taskId")
            .exchange()
            .expectStatus().isNoContent

        // Then: 조회 시 404 (소프트 삭제되어 활성 데이터에서 제외)
        webTestClient.get()
            .uri("/api/v1/gantt/tasks/$taskId")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Gantt Task 복원 성공`() {
        // Given: Gantt Task 생성 및 삭제
        val todoId = createTodo("Todo for Gantt Restore Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val taskId = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS)
                )
            )
            .exchange()
            .expectBody<GanttTaskResponse>()
            .returnResult()
            .responseBody!!
            .id

        // 삭제
        webTestClient.delete()
            .uri("/api/v1/gantt/tasks/$taskId")
            .exchange()
            .expectStatus().isNoContent

        // When: Gantt Task 복원
        webTestClient.post()
            .uri("/api/v1/gantt/tasks/$taskId/restore")
            .exchange()
            .expectStatus().isOk

        // Then: 다시 조회 가능
        webTestClient.get()
            .uri("/api/v1/gantt/tasks/$taskId")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Gantt Task 생성 시 startDate가 endDate보다 크면 400 에러`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for Gantt Validation Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val startDate = now.plus(10, ChronoUnit.DAYS)
        val endDate = now  // endDate < startDate

        // When: 잘못된 날짜로 Gantt Task 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = startDate,
                    endDate = endDate,
                    progress = 0
                )
            )
            .exchange()

        // Then: 400 Bad Request
        response.expectStatus().isBadRequest
    }

    @Test
    fun `Gantt Task 생성 시 progress가 100 초과하면 400 에러`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for Progress Validation Test")

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        // When: progress > 100으로 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = todoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS),
                    progress = 101
                )
            )
            .exchange()

        // Then: 400 Bad Request
        response.expectStatus().isBadRequest
    }

    @Test
    fun `Gantt Task 생성 시 존재하지 않는 todoId면 404 에러`() {
        // Given: 존재하지 않는 UUID
        val nonExistentTodoId = UUID.randomUUID()

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        // When: 존재하지 않는 todoId로 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/gantt/tasks")
            .bodyValue(
                CreateGanttTaskRequest(
                    todoId = nonExistentTodoId,
                    startDate = now,
                    endDate = now.plus(5, ChronoUnit.DAYS)
                )
            )
            .exchange()

        // Then: 404 Not Found
        response.expectStatus().isNotFound
    }

    @Test
    fun `존재하지 않는 Gantt Task 조회 시 404 에러`() {
        // Given: 존재하지 않는 UUID
        val nonExistentTaskId = UUID.randomUUID()

        // When: 존재하지 않는 taskId로 조회 시도
        val response = webTestClient.get()
            .uri("/api/v1/gantt/tasks/$nonExistentTaskId")
            .exchange()

        // Then: 404 Not Found
        response.expectStatus().isNotFound
    }
}
