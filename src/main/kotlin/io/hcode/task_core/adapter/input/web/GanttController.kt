package io.hcode.task_core.adapter.input.web

import io.hcode.task_core.adapter.input.web.dto.gantt.CreateGanttTaskRequest
import io.hcode.task_core.adapter.input.web.dto.gantt.GanttTaskResponse
import io.hcode.task_core.adapter.input.web.dto.gantt.UpdateGanttTaskRequest
import io.hcode.task_core.domain.port.input.GanttCommandPort
import io.hcode.task_core.domain.port.input.GanttQueryPort
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Gantt Task REST Controller (Inbound Adapter)
 *
 * 비즈니스 의미: 간트 차트 상의 태스크 관리를 위한 HTTP API를 제공합니다.
 *
 * 기술적 배경:
 * - Spring WebFlux 기반의 반응형(Reactive) REST API
 * - Kotlin Coroutines를 활용한 비동기 처리
 * - DTO 검증을 통한 입력 데이터 안정성 확보
 */
@RestController
@RequestMapping("/api/v1/gantt/tasks")
class GanttController(
    private val ganttCommandPort: GanttCommandPort,
    private val ganttQueryPort: GanttQueryPort
) {

    /**
     * 새로운 Gantt Task 생성
     *
     * 비즈니스 의미: Todo를 간트 차트에 추가하여 일정 관리를 시작합니다.
     *
     * @param request Gantt Task 생성 요청 DTO (todoId, 시작일, 종료일, 진행률)
     * @return 생성된 Gantt Task의 상세 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createGanttTask(
        @Valid @RequestBody request: CreateGanttTaskRequest
    ): GanttTaskResponse {
        val ganttTask = ganttCommandPort.createGanttTask(request)
        return GanttTaskResponse.from(ganttTask)
    }

    /**
     * ID로 Gantt Task 조회
     *
     * 비즈니스 의미: 특정 간트 태스크의 일정 정보를 조회합니다.
     *
     * @param id Gantt Task의 고유 식별자
     * @param includeDeleted 삭제된 Gantt Task 포함 여부 (기본값: false)
     * @return Gantt Task의 상세 정보
     */
    @GetMapping("/{id}")
    suspend fun getGanttTaskById(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): GanttTaskResponse {
        val ganttTask = ganttQueryPort.getGanttTaskById(id, includeDeleted)
            ?: throw NoSuchElementException("GanttTask not found: $id")
        return GanttTaskResponse.from(ganttTask)
    }

    /**
     * Todo ID로 Gantt Task 조회
     *
     * 비즈니스 의미: 특정 Todo의 간트 차트 일정을 조회합니다.
     *
     * @param todoId Todo의 고유 식별자
     * @param includeDeleted 삭제된 Gantt Task 포함 여부 (기본값: false)
     * @return Gantt Task의 상세 정보
     */
    @GetMapping("/todo/{todoId}")
    suspend fun getGanttTaskByTodoId(
        @PathVariable todoId: UUID,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): GanttTaskResponse {
        val ganttTask = ganttQueryPort.getGanttTaskByTodoId(todoId, includeDeleted)
            ?: throw NoSuchElementException("GanttTask not found for Todo: $todoId")
        return GanttTaskResponse.from(ganttTask)
    }

    /**
     * 모든 Gantt Task 조회 (또는 특정 Todo의 Gantt Task 조회)
     *
     * 비즈니스 의미: 전체 간트 차트의 일정 목록을 조회하거나, 특정 Todo의 간트 태스크를 필터링합니다.
     *
     * @param todoId 필터링할 Todo ID (선택사항)
     * @param includeDeleted 삭제된 Gantt Task 포함 여부 (기본값: false)
     * @return Gantt Task 목록 스트림
     */
    @GetMapping
    fun getAllGanttTasks(
        @RequestParam(required = false) todoId: UUID?,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): Flow<GanttTaskResponse> {
        return if (todoId != null) {
            ganttQueryPort.getGanttTasksByTodoIds(listOf(todoId))
                .map { GanttTaskResponse.from(it) }
        } else {
            ganttQueryPort.getAllGanttTasks(includeDeleted)
                .map { GanttTaskResponse.from(it) }
        }
    }

    /**
     * Gantt Task 수정
     *
     * 비즈니스 의미: 기존 간트 태스크의 일정 정보를 변경합니다.
     *
     * @param id 수정할 Gantt Task의 ID
     * @param request Gantt Task 수정 요청 DTO (시작일, 종료일, 진행률)
     * @return 수정된 Gantt Task의 상세 정보
     */
    @PutMapping("/{id}")
    suspend fun updateGanttTask(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateGanttTaskRequest
    ): GanttTaskResponse {
        val ganttTask = ganttCommandPort.updateGanttTask(id, request)
        return GanttTaskResponse.from(ganttTask)
    }

    /**
     * Gantt Task 삭제 (소프트 삭제)
     *
     * 비즈니스 의미: 간트 차트에서 태스크를 삭제 상태로 변경합니다. 실제 데이터는 보존됩니다.
     *
     * @param id 삭제할 Gantt Task의 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteGanttTask(@PathVariable id: UUID) {
        ganttCommandPort.deleteGanttTask(id)
    }

    /**
     * Gantt Task 복원
     *
     * 비즈니스 의미: 삭제된 간트 태스크를 다시 활성화합니다.
     *
     * @param id 복원할 Gantt Task의 ID
     * @return 복원된 Gantt Task의 상세 정보
     */
    @PostMapping("/{id}/restore")
    suspend fun restoreGanttTask(@PathVariable id: UUID): GanttTaskResponse {
        val ganttTask = ganttCommandPort.restoreGanttTask(id)
        return GanttTaskResponse.from(ganttTask)
    }
}
