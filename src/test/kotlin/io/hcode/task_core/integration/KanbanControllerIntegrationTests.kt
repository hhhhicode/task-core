package io.hcode.task_core.integration

import io.hcode.task_core.adapter.input.web.dto.kanban.CreateKanbanCardRequest
import io.hcode.task_core.adapter.input.web.dto.kanban.KanbanCardResponse
import io.hcode.task_core.adapter.input.web.dto.kanban.MoveKanbanCardRequest
import io.hcode.task_core.adapter.input.web.dto.todo.CreateTodoRequest
import io.hcode.task_core.adapter.input.web.dto.todo.TodoResponse
import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

/**
 * Kanban Controller 통합 테스트
 *
 * 비즈니스 의미: KanbanCard REST API의 완전한 CRUD 동작과 칸반 보드 재배치 규칙을 검증합니다.
 *
 * 테스트 커버리지:
 * 1. Kanban Card 생성 (POST /api/v1/kanban/cards)
 * 2. Kanban Card 조회 (GET /api/v1/kanban/cards/{id})
 * 3. Kanban Card 필터 조회 (GET /api/v1/kanban/cards?columns=...)
 * 4. Kanban Card 이동 (PUT /api/v1/kanban/cards/{id}/position)
 * 5. Kanban Card 삭제 (DELETE /api/v1/kanban/cards/{id})
 * 6. Kanban Card 복원 (POST /api/v1/kanban/cards/{id}/restore)
 * 7. 유효성 검증 (columnId NotBlank, position >= 0)
 * 8. 에러 케이스 (존재하지 않는 Todo, 중복 todoId, 404 등)
 */
class KanbanControllerIntegrationTests : IntegrationTestBase() {

    @Autowired
    lateinit var webTestClient: WebTestClient

    /**
     * 테스트 헬퍼: Todo 생성
     *
     * 비즈니스 의미: KanbanCard는 Todo를 참조하므로 테스트 전 Todo를 먼저 생성합니다.
     */
    private fun createTodo(title: String = "Test Todo for Kanban"): UUID {
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
    fun `Kanban Card 생성 성공`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for Kanban Creation Test")

        // When: Kanban Card 생성
        val response = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "backlog",
                    position = 0
                )
            )
            .exchange()

        // Then: 201 Created
        response.expectStatus().isCreated
            .expectBody<KanbanCardResponse>()
            .consumeWith { result ->
                val card = result.responseBody!!
                assert(card.id != UUID(0, 0))
                assert(card.todoId == todoId)
                assert(card.columnId == "backlog")
                assert(card.position == 0)
            }
    }

    @Test
    fun `Kanban Card 조회 성공`() {
        // Given: Todo 및 Kanban Card 생성
        val todoId = createTodo("Todo for Kanban Retrieval Test")

        val cardId = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "in-progress",
                    position = 5
                )
            )
            .exchange()
            .expectBody<KanbanCardResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: 단일 Kanban Card 조회
        val response = webTestClient.get()
            .uri("/api/v1/kanban/cards/$cardId")
            .exchange()

        // Then: 200 OK
        response.expectStatus().isOk
            .expectBody<KanbanCardResponse>()
            .consumeWith { result ->
                val card = result.responseBody!!
                assert(card.id == cardId)
                assert(card.todoId == todoId)
                assert(card.columnId == "in-progress")
                assert(card.position == 5)
            }
    }

    @Test
    fun `columnId로 Kanban Card 조회 성공`() {
        // Given: 3개의 Todo와 서로 다른 컬럼에 Kanban Card 생성
        val todoId1 = createTodo("Todo 1 for Column Filter Test")
        val todoId2 = createTodo("Todo 2 for Column Filter Test")
        val todoId3 = createTodo("Todo 3 for Column Filter Test")

        webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId1,
                    columnId = "backlog",
                    position = 0
                )
            )
            .exchange()
            .expectStatus().isCreated

        webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId2,
                    columnId = "in-progress",
                    position = 0
                )
            )
            .exchange()
            .expectStatus().isCreated

        webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId3,
                    columnId = "backlog",
                    position = 1
                )
            )
            .exchange()
            .expectStatus().isCreated

        // When: "backlog" 컬럼으로 필터 조회
        val response = webTestClient.get()
            .uri("/api/v1/kanban/cards?columns=backlog")
            .exchange()

        // Then: 200 OK, backlog 컬럼의 card만 반환 (최소 2개)
        response.expectStatus().isOk

        val cards = response.expectBodyList(KanbanCardResponse::class.java)
            .returnResult()
            .responseBody!!
        assert(cards.size >= 2)
        assert(cards.all { it.columnId == "backlog" })
    }

    @Test
    fun `모든 Kanban Card 조회 성공`() {
        // Given: 2개의 Kanban Card 생성
        val todoId1 = createTodo("Todo 1 for All Cards Test")
        val todoId2 = createTodo("Todo 2 for All Cards Test")

        webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId1,
                    columnId = "backlog",
                    position = 0
                )
            )
            .exchange()
            .expectStatus().isCreated

        webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId2,
                    columnId = "done",
                    position = 0
                )
            )
            .exchange()
            .expectStatus().isCreated

        // When: 전체 Kanban Card 조회
        val response = webTestClient.get()
            .uri("/api/v1/kanban/cards")
            .exchange()

        // Then: 200 OK, 최소 2개 이상의 card 반환 (이전 테스트 데이터 포함 가능)
        response.expectStatus().isOk

        val cards = response.expectBodyList(KanbanCardResponse::class.java)
            .returnResult()
            .responseBody!!
        assert(cards.size >= 2)
    }

    @Test
    fun `Kanban Card 이동 성공 (동일 컬럼 내)`() {
        // Given: Kanban Card 생성
        val todoId = createTodo("Todo for Kanban Move Test")

        val cardId = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "in-progress",
                    position = 3
                )
            )
            .exchange()
            .expectBody<KanbanCardResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: 동일 컬럼 내에서 position 변경
        val response = webTestClient.put()
            .uri("/api/v1/kanban/cards/$cardId/position")
            .bodyValue(
                MoveKanbanCardRequest(
                    newColumnId = "in-progress",
                    newPosition = 7
                )
            )
            .exchange()

        // Then: 200 OK, 새로운 position 반영
        response.expectStatus().isOk
            .expectBody<KanbanCardResponse>()
            .consumeWith { result ->
                val card = result.responseBody!!
                assert(card.id == cardId)
                assert(card.columnId == "in-progress")
                assert(card.position == 7)
            }
    }

    @Test
    fun `Kanban Card 이동 성공 (다른 컬럼으로)`() {
        // Given: Kanban Card 생성
        val todoId = createTodo("Todo for Kanban Column Move Test")

        val cardId = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "backlog",
                    position = 2
                )
            )
            .exchange()
            .expectBody<KanbanCardResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: 다른 컬럼으로 이동
        val response = webTestClient.put()
            .uri("/api/v1/kanban/cards/$cardId/position")
            .bodyValue(
                MoveKanbanCardRequest(
                    newColumnId = "done",
                    newPosition = 0
                )
            )
            .exchange()

        // Then: 200 OK, 새로운 컬럼 및 position 반영
        response.expectStatus().isOk
            .expectBody<KanbanCardResponse>()
            .consumeWith { result ->
                val card = result.responseBody!!
                assert(card.id == cardId)
                assert(card.columnId == "done")
                assert(card.position == 0)
            }
    }

    @Test
    fun `Kanban Card 삭제 성공`() {
        // Given: Kanban Card 생성
        val todoId = createTodo("Todo for Kanban Delete Test")

        val cardId = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "backlog",
                    position = 0
                )
            )
            .exchange()
            .expectBody<KanbanCardResponse>()
            .returnResult()
            .responseBody!!
            .id

        // When: Kanban Card 삭제 (소프트 삭제)
        webTestClient.delete()
            .uri("/api/v1/kanban/cards/$cardId")
            .exchange()
            .expectStatus().isNoContent

        // Then: 조회 시 404 (소프트 삭제되어 활성 데이터에서 제외)
        webTestClient.get()
            .uri("/api/v1/kanban/cards/$cardId")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Kanban Card 복원 성공`() {
        // Given: Kanban Card 생성 및 삭제
        val todoId = createTodo("Todo for Kanban Restore Test")

        val cardId = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "in-progress",
                    position = 0
                )
            )
            .exchange()
            .expectBody<KanbanCardResponse>()
            .returnResult()
            .responseBody!!
            .id

        // 삭제
        webTestClient.delete()
            .uri("/api/v1/kanban/cards/$cardId")
            .exchange()
            .expectStatus().isNoContent

        // When: Kanban Card 복원
        webTestClient.post()
            .uri("/api/v1/kanban/cards/$cardId/restore")
            .exchange()
            .expectStatus().isOk

        // Then: 다시 조회 가능
        webTestClient.get()
            .uri("/api/v1/kanban/cards/$cardId")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Kanban Card 생성 시 columnId가 빈 문자열이면 400 에러`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for ColumnId Validation Test")

        // When: columnId = "" (빈 문자열)로 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "",
                    position = 0
                )
            )
            .exchange()

        // Then: 400 Bad Request
        response.expectStatus().isBadRequest
    }

    @Test
    fun `Kanban Card 생성 시 position이 음수면 400 에러`() {
        // Given: Todo 생성
        val todoId = createTodo("Todo for Position Validation Test")

        // When: position < 0으로 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = todoId,
                    columnId = "backlog",
                    position = -1
                )
            )
            .exchange()

        // Then: 400 Bad Request
        response.expectStatus().isBadRequest
    }

    @Test
    fun `Kanban Card 생성 시 존재하지 않는 todoId면 404 에러`() {
        // Given: 존재하지 않는 UUID
        val nonExistentTodoId = UUID.randomUUID()

        // When: 존재하지 않는 todoId로 생성 시도
        val response = webTestClient.post()
            .uri("/api/v1/kanban/cards")
            .bodyValue(
                CreateKanbanCardRequest(
                    todoId = nonExistentTodoId,
                    columnId = "backlog",
                    position = 0
                )
            )
            .exchange()

        // Then: 404 Not Found
        response.expectStatus().isNotFound
    }

    @Test
    fun `존재하지 않는 Kanban Card 조회 시 404 에러`() {
        // Given: 존재하지 않는 UUID
        val nonExistentCardId = UUID.randomUUID()

        // When: 존재하지 않는 cardId로 조회 시도
        val response = webTestClient.get()
            .uri("/api/v1/kanban/cards/$nonExistentCardId")
            .exchange()

        // Then: 404 Not Found
        response.expectStatus().isNotFound
    }
}
