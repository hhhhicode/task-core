package io.hcode.task_core.adapter.input.web

import io.hcode.task_core.adapter.input.web.dto.kanban.CreateKanbanCardRequest
import io.hcode.task_core.adapter.input.web.dto.kanban.KanbanCardResponse
import io.hcode.task_core.adapter.input.web.dto.kanban.MoveKanbanCardRequest
import io.hcode.task_core.domain.port.input.KanbanCommandPort
import io.hcode.task_core.domain.port.input.KanbanQueryPort
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Kanban Card REST Controller (Inbound Adapter)
 *
 * 비즈니스 의미: 칸반 보드 상의 카드 관리를 위한 HTTP API를 제공합니다.
 *
 * 기술적 배경:
 * - Spring WebFlux 기반의 반응형(Reactive) REST API
 * - Kotlin Coroutines를 활용한 비동기 처리
 * - DTO 검증을 통한 입력 데이터 안정성 확보
 */
@RestController
@RequestMapping("/api/v1/kanban/cards")
class KanbanController(
    private val kanbanCommandPort: KanbanCommandPort,
    private val kanbanQueryPort: KanbanQueryPort
) {

    /**
     * 새로운 Kanban Card 생성
     *
     * 비즈니스 의미: Todo를 칸반 보드에 추가하여 워크플로우 관리를 시작합니다.
     *
     * @param request Kanban Card 생성 요청 DTO (todoId, columnId, position)
     * @return 생성된 Kanban Card의 상세 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createKanbanCard(
        @Valid @RequestBody request: CreateKanbanCardRequest
    ): KanbanCardResponse {
        val kanbanCard = kanbanCommandPort.createKanbanCard(request)
        return KanbanCardResponse.from(kanbanCard)
    }

    /**
     * ID로 Kanban Card 조회
     *
     * 비즈니스 의미: 특정 칸반 카드의 정보를 조회합니다.
     *
     * @param id Kanban Card의 고유 식별자
     * @param includeDeleted 삭제된 Kanban Card 포함 여부 (기본값: false)
     * @return Kanban Card의 상세 정보
     */
    @GetMapping("/{id}")
    suspend fun getKanbanCardById(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): KanbanCardResponse {
        val kanbanCard = kanbanQueryPort.getKanbanCardById(id, includeDeleted)
            ?: throw NoSuchElementException("KanbanCard not found: $id")
        return KanbanCardResponse.from(kanbanCard)
    }

    /**
     * Todo ID로 Kanban Card 조회
     *
     * 비즈니스 의미: 특정 Todo의 칸반 카드를 조회합니다.
     *
     * @param todoId Todo의 고유 식별자
     * @param includeDeleted 삭제된 Kanban Card 포함 여부 (기본값: false)
     * @return Kanban Card의 상세 정보
     */
    @GetMapping("/todo/{todoId}")
    suspend fun getKanbanCardByTodoId(
        @PathVariable todoId: UUID,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): KanbanCardResponse {
        val kanbanCard = kanbanQueryPort.getKanbanCardByTodoId(todoId, includeDeleted)
            ?: throw NoSuchElementException("KanbanCard not found for Todo: $todoId")
        return KanbanCardResponse.from(kanbanCard)
    }

    /**
     * Column ID로 Kanban Card 조회
     *
     * 비즈니스 의미: 특정 워크플로우 단계에 속한 모든 카드를 조회합니다.
     *
     * @param columnId 칸반 컬럼 ID
     * @return Kanban Card 목록 스트림
     */
    @GetMapping("/column/{columnId}")
    fun getKanbanCardsByColumn(
        @PathVariable columnId: String
    ): Flow<KanbanCardResponse> {
        return kanbanQueryPort.getKanbanCardsByColumn(columnId)
            .map { KanbanCardResponse.from(it) }
    }

    /**
     * 모든 Kanban Card 조회 (또는 특정 컬럼으로 필터링)
     *
     * 비즈니스 의미: 전체 칸반 보드의 카드 목록을 조회하거나, 특정 컬럼(들)의 카드만 필터링합니다.
     *
     * @param columns 필터링할 컬럼 ID (선택사항, 쉼표로 구분된 문자열)
     * @param includeDeleted 삭제된 Kanban Card 포함 여부 (기본값: false)
     * @return Kanban Card 목록 스트림
     */
    @GetMapping
    fun getAllKanbanCards(
        @RequestParam(required = false) columns: String?,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): Flow<KanbanCardResponse> {
        return if (columns != null) {
            val columnIds = columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            kanbanQueryPort.getKanbanCardsByColumns(columnIds)
                .map { KanbanCardResponse.from(it) }
        } else {
            kanbanQueryPort.getAllKanbanCards(includeDeleted)
                .map { KanbanCardResponse.from(it) }
        }
    }

    /**
     * Kanban Card 이동
     *
     * 비즈니스 의미: 칸반 카드를 다른 컬럼으로 이동하거나 같은 컬럼 내에서 위치를 변경합니다.
     *
     * @param id 이동할 Kanban Card의 ID
     * @param request Kanban Card 이동 요청 DTO (newColumnId, newPosition)
     * @return 이동된 Kanban Card의 상세 정보
     */
    @PutMapping("/{id}/position")
    suspend fun moveKanbanCard(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MoveKanbanCardRequest
    ): KanbanCardResponse {
        val kanbanCard = kanbanCommandPort.moveKanbanCard(id, request)
        return KanbanCardResponse.from(kanbanCard)
    }

    /**
     * Kanban Card 삭제 (소프트 삭제)
     *
     * 비즈니스 의미: 칸반 보드에서 카드를 삭제 상태로 변경합니다. 실제 데이터는 보존됩니다.
     *
     * @param id 삭제할 Kanban Card의 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteKanbanCard(@PathVariable id: UUID) {
        kanbanCommandPort.deleteKanbanCard(id)
    }

    /**
     * Kanban Card 복원
     *
     * 비즈니스 의미: 삭제된 칸반 카드를 다시 활성화합니다.
     *
     * @param id 복원할 Kanban Card의 ID
     * @return 복원된 Kanban Card의 상세 정보
     */
    @PostMapping("/{id}/restore")
    suspend fun restoreKanbanCard(@PathVariable id: UUID): KanbanCardResponse {
        val kanbanCard = kanbanCommandPort.restoreKanbanCard(id)
        return KanbanCardResponse.from(kanbanCard)
    }
}
