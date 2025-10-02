package io.hcode.task_core.adapter.input.web

import io.hcode.task_core.adapter.output.event.DomainEvent
import io.hcode.task_core.adapter.output.event.SseEventPublishAdapter
import io.hcode.task_core.infrastructure.config.SseConfigProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID

/**
 * SSE (Server-Sent Events) Controller (Inbound Adapter)
 *
 * 비즈니스 의미: 클라이언트가 실시간 도메인 이벤트를 구독할 수 있는 SSE 엔드포인트를 제공합니다.
 *
 * 기술적 배경:
 * - SSE는 HTTP 기반 단방향(서버→클라이언트) 실시간 통신 프로토콜
 * - text/event-stream MIME 타입 사용
 * - 자동 재연결 지원 (클라이언트 측)
 * - WebSocket보다 단순하며 HTTP 프록시/로드밸런서 친화적
 *
 * PRD 요구사항 (섹션 3.4):
 * - 도메인별 분리 엔드포인트 (kanban, gantt, todos)
 * - 쿼리 파라미터 기반 필터링
 * - 모노톤 증가 이벤트 ID
 * - Last-Event-ID 재구독 지원
 * - 15초 하트비트
 *
 * 사용 예시:
 * ```javascript
 * // Kanban 이벤트만 구독 (특정 컬럼들)
 * const eventSource = new EventSource('/api/v1/stream/kanban?columns=TODO,IN_PROGRESS');
 *
 * // Gantt 이벤트만 구독 (특정 Todo들)
 * const eventSource = new EventSource('/api/v1/stream/gantt?todoIds=uuid1,uuid2');
 *
 * // 모든 Todo 이벤트 구독
 * const eventSource = new EventSource('/api/v1/stream/todos');
 *
 * // 재구독 (마지막 이벤트 ID부터)
 * eventSource.addEventListener('open', () => {
 *   // Last-Event-ID 헤더는 브라우저가 자동으로 전송
 * });
 * ```
 */
@RestController
@RequestMapping("/api/v1/stream")
class SseController(
    private val eventPublishAdapter: SseEventPublishAdapter,
    private val sseConfigProperties: SseConfigProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Kanban 이벤트 스트림 (도메인 분리 엔드포인트)
     *
     * 비즈니스 의미: Kanban 보드 관련 이벤트만 선택적으로 구독합니다.
     *
     * 기술적 세부사항:
     * - columns 파라미터로 특정 컬럼의 이벤트만 필터링
     * - Last-Event-ID 헤더 지원으로 재구독 시 놓친 이벤트 복구
     * - 모노톤 증가 이벤트 ID로 순서 보장
     *
     * @param columns 필터링할 컬럼 ID 목록 (쉼표 구분, 선택사항)
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트 ID (재구독용)
     * @return Kanban 이벤트 SSE 스트림
     */
    @GetMapping("/kanban", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamKanbanEvents(
        @RequestParam(required = false) columns: String?,
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
    ): Flux<ServerSentEvent<DomainEvent>> {
        logger.info("New Kanban SSE client connected (columns: $columns, lastEventId: $lastEventId)")

        val columnIds = columns?.split(",")?.map { it.trim() }?.toSet()

        return createEventStream(lastEventId) { event ->
            when (event) {
                is DomainEvent.KanbanCardCreated,
                is DomainEvent.KanbanCardMoved,
                is DomainEvent.KanbanCardDeleted,
                is DomainEvent.KanbanCardRestored -> {
                    // 컬럼 필터링 적용
                    if (columnIds == null) {
                        true // 모든 Kanban 이벤트
                    } else {
                        val cardColumnId = when (event) {
                            is DomainEvent.KanbanCardCreated -> event.kanbanCard.columnId
                            is DomainEvent.KanbanCardMoved -> event.kanbanCard.columnId
                            is DomainEvent.KanbanCardDeleted -> event.kanbanCard.columnId
                            is DomainEvent.KanbanCardRestored -> event.kanbanCard.columnId
                            else -> null
                        }
                        cardColumnId in columnIds
                    }
                }
                else -> false // Kanban 이벤트가 아님
            }
        }
    }

    /**
     * Gantt 이벤트 스트림 (도메인 분리 엔드포인트)
     *
     * 비즈니스 의미: Gantt 차트 관련 이벤트만 선택적으로 구독합니다.
     *
     * 기술적 세부사항:
     * - todoIds 파라미터로 특정 Todo에 연결된 Gantt Task만 필터링
     * - Last-Event-ID 헤더 지원으로 재구독 시 놓친 이벤트 복구
     * - 모노톤 증가 이벤트 ID로 순서 보장
     *
     * @param todoIds 필터링할 Todo ID 목록 (쉼표 구분, 선택사항)
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트 ID (재구독용)
     * @return Gantt 이벤트 SSE 스트림
     */
    @GetMapping("/gantt", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamGanttEvents(
        @RequestParam(required = false) todoIds: String?,
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
    ): Flux<ServerSentEvent<DomainEvent>> {
        logger.info("New Gantt SSE client connected (todoIds: $todoIds, lastEventId: $lastEventId)")

        val todoIdSet = todoIds?.split(",")?.mapNotNull {
            try {
                UUID.fromString(it.trim())
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID in todoIds: ${it.trim()}")
                null
            }
        }?.toSet()

        return createEventStream(lastEventId) { event ->
            when (event) {
                is DomainEvent.GanttTaskCreated,
                is DomainEvent.GanttTaskUpdated,
                is DomainEvent.GanttTaskDeleted,
                is DomainEvent.GanttTaskRestored -> {
                    // Todo ID 필터링 적용
                    if (todoIdSet == null) {
                        true // 모든 Gantt 이벤트
                    } else {
                        val taskTodoId = when (event) {
                            is DomainEvent.GanttTaskCreated -> event.ganttTask.todoId
                            is DomainEvent.GanttTaskUpdated -> event.ganttTask.todoId
                            is DomainEvent.GanttTaskDeleted -> event.ganttTask.todoId
                            is DomainEvent.GanttTaskRestored -> event.ganttTask.todoId
                            else -> null
                        }
                        taskTodoId in todoIdSet
                    }
                }
                else -> false // Gantt 이벤트가 아님
            }
        }
    }

    /**
     * Todo 이벤트 스트림 (도메인 분리 엔드포인트)
     *
     * 비즈니스 의미: Todo 관련 이벤트만 선택적으로 구독합니다.
     *
     * 기술적 세부사항:
     * - Todo CRUD 이벤트만 전송
     * - Last-Event-ID 헤더 지원으로 재구독 시 놓친 이벤트 복구
     * - 모노톤 증가 이벤트 ID로 순서 보장
     *
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트 ID (재구독용)
     * @return Todo 이벤트 SSE 스트림
     */
    @GetMapping("/todos", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamTodoEvents(
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
    ): Flux<ServerSentEvent<DomainEvent>> {
        logger.info("New Todo SSE client connected (lastEventId: $lastEventId)")

        return createEventStream(lastEventId) { event ->
            when (event) {
                is DomainEvent.TodoCreated,
                is DomainEvent.TodoUpdated,
                is DomainEvent.TodoDeleted,
                is DomainEvent.TodoRestored -> true
                else -> false
            }
        }
    }

    /**
     * 레거시 엔드포인트 (하위 호환성)
     *
     * 비즈니스 의미: 모든 도메인 이벤트를 브로드캐스트합니다.
     *
     * @deprecated 새로운 코드에서는 도메인별 분리 엔드포인트를 사용하세요.
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트 ID (재구독용)
     * @return 모든 도메인 이벤트 SSE 스트림
     */
    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Deprecated("Use domain-specific endpoints: /api/v1/stream/{kanban|gantt|todos}")
    fun streamAllEvents(
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
    ): Flux<ServerSentEvent<DomainEvent>> {
        logger.info("New SSE client connected (all events, lastEventId: $lastEventId)")
        return createEventStream(lastEventId) { true } // 모든 이벤트
    }

    /**
     * SSE 이벤트 스트림 생성 헬퍼
     *
     * 비즈니스 의미: 재구독 지원 및 필터링이 적용된 SSE 스트림을 생성합니다.
     *
     * 기술적 세부사항:
     * - Last-Event-ID 기반 재구독: 놓친 이벤트 복구
     * - 필터 함수로 이벤트 선택적 전송
     * - 설정 기반 하트비트 (기본 15초)
     *
     * @param lastEventId 재구독 시 시작 이벤트 ID
     * @param filter 이벤트 필터링 함수 (true = 전송, false = 스킵)
     * @return 필터링된 SSE 스트림
     */
    private fun createEventStream(
        lastEventId: String?,
        filter: (DomainEvent) -> Boolean
    ): Flux<ServerSentEvent<DomainEvent>> {
        val lastId = lastEventId?.toLongOrNull() ?: 0L

        return eventPublishAdapter.eventSink.asFlux()
            .filter { event ->
                // Last-Event-ID 이후의 이벤트만 전송
                event.eventId > lastId && filter(event)
            }
            .map { event ->
                ServerSentEvent.builder<DomainEvent>()
                    .id(event.eventId.toString())
                    .event(event.type)
                    .data(event)
                    .build()
            }
            .mergeWith(heartbeat())
            .doOnCancel {
                logger.info("SSE client disconnected")
            }
            .doOnError { error ->
                logger.error("SSE stream error", error)
            }
    }

    /**
     * Heartbeat 이벤트 생성
     *
     * 비즈니스 의미: 프록시/로드밸런서 타임아웃 방지와 연결 상태 확인을 위한 주기적 신호
     *
     * 기술적 세부사항:
     * - 설정 파일에서 간격 읽기 (기본 15초, PRD 요구사항)
     * - comment 타입 이벤트로 클라이언트 측 처리 불필요
     * - 일부 프록시는 30초 이상 데이터 전송이 없으면 연결을 끊음
     *
     * @return Heartbeat SSE 이벤트 스트림
     */
    private fun heartbeat(): Flux<ServerSentEvent<DomainEvent>> {
        val heartbeatSeconds = sseConfigProperties.heartbeatSeconds
        logger.debug("Heartbeat interval: $heartbeatSeconds seconds")

        return Flux.interval(Duration.ofSeconds(heartbeatSeconds))
            .map {
                ServerSentEvent.builder<DomainEvent>()
                    .comment("heartbeat")
                    .build()
            }
    }
}
