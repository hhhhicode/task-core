package io.hcode.task_core.adapter.output.persistence

import io.hcode.task_core.domain.model.GanttTask
import io.hcode.task_core.domain.port.output.GanttStorePort
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * GanttTask 영속성 어댑터 (Outbound Adapter)
 *
 * 비즈니스 의미: GanttTask 엔티티의 데이터베이스 저장소 구현체입니다.
 *
 * 기술적 배경:
 * - R2DBC로 비동기 non-blocking 방식으로 데이터베이스 작업을 수행합니다.
 * - 소프트 삭제 패턴을 지원합니다.
 * - Todo와 1:1 관계를 유지합니다 (todo_id는 유니크).
 */
@Repository
class GanttStoreAdapter(
    private val databaseClient: DatabaseClient
) : GanttStorePort {

    override suspend fun create(ganttTask: GanttTask): UUID {
        val sql = """
            INSERT INTO gantt_tasks (id, todo_id, start_date, end_date, progress, deleted_at)
            VALUES (:id, :todoId, :startDate, :endDate, :progress, :deletedAt)
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", ganttTask.id)
            .bind("todoId", ganttTask.todoId)
            .bind("startDate", ganttTask.startDate.toOffsetDateTime())
            .bind("endDate", ganttTask.endDate.toOffsetDateTime())
            .bind("progress", ganttTask.progress)

        // Handle nullable deletedAt
        spec = if (ganttTask.deletedAt != null) {
            spec.bind("deletedAt", ganttTask.deletedAt.toOffsetDateTime())
        } else {
            spec.bindNull("deletedAt", OffsetDateTime::class.java)
        }

        spec.then().awaitFirstOrNull()

        return ganttTask.id
    }

    override suspend fun findByIdActive(id: UUID): GanttTask? {
        val sql = """
            SELECT id, todo_id, start_date, end_date, progress, deleted_at
            FROM gantt_tasks
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toGanttTask() }
            .one()
            .awaitFirstOrNull()
    }

    override suspend fun findById(id: UUID, includeDeleted: Boolean): GanttTask? {
        val sql = if (includeDeleted) {
            """
                SELECT id, todo_id, start_date, end_date, progress, deleted_at
                FROM gantt_tasks
                WHERE id = :id
            """.trimIndent()
        } else {
            """
                SELECT id, todo_id, start_date, end_date, progress, deleted_at
                FROM gantt_tasks
                WHERE id = :id AND deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toGanttTask() }
            .one()
            .awaitFirstOrNull()
    }

    override suspend fun findByTodoIdActive(todoId: UUID): GanttTask? {
        val sql = """
            SELECT id, todo_id, start_date, end_date, progress, deleted_at
            FROM gantt_tasks
            WHERE todo_id = :todoId AND deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("todoId", todoId)
            .map { row, _ -> row.toGanttTask() }
            .one()
            .awaitFirstOrNull()
    }

    override fun findAllActive(): Flow<GanttTask> {
        val sql = """
            SELECT id, todo_id, start_date, end_date, progress, deleted_at
            FROM gantt_tasks
            WHERE deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .map { row, _ -> row.toGanttTask() }
            .all()
            .asFlow()
    }

    override fun findAll(includeDeleted: Boolean): Flow<GanttTask> {
        val sql = if (includeDeleted) {
            """
                SELECT id, todo_id, start_date, end_date, progress, deleted_at
                FROM gantt_tasks
            """.trimIndent()
        } else {
            """
                SELECT id, todo_id, start_date, end_date, progress, deleted_at
                FROM gantt_tasks
                WHERE deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .map { row, _ -> row.toGanttTask() }
            .all()
            .asFlow()
    }

    override fun findByTodoIdsActive(todoIds: Collection<UUID>): Flow<GanttTask> {
        if (todoIds.isEmpty()) {
            return kotlinx.coroutines.flow.emptyFlow()
        }

        // R2DBC doesn't support array binding for IN clause directly
        // We need to construct the SQL with placeholders dynamically
        val placeholders = todoIds.indices.joinToString(",") { ":todoId$it" }
        val sql = """
            SELECT id, todo_id, start_date, end_date, progress, deleted_at
            FROM gantt_tasks
            WHERE todo_id IN ($placeholders) AND deleted_at IS NULL
        """.trimIndent()

        var spec = databaseClient.sql(sql)
        todoIds.forEachIndexed { index, todoId ->
            spec = spec.bind("todoId$index", todoId)
        }

        return spec.map { row, _ -> row.toGanttTask() }
            .all()
            .asFlow()
    }

    override suspend fun update(ganttTask: GanttTask): Boolean {
        val sql = """
            UPDATE gantt_tasks
            SET start_date = :startDate,
                end_date = :endDate,
                progress = :progress,
                deleted_at = :deletedAt
            WHERE id = :id
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", ganttTask.id)
            .bind("startDate", ganttTask.startDate.toOffsetDateTime())
            .bind("endDate", ganttTask.endDate.toOffsetDateTime())
            .bind("progress", ganttTask.progress)

        // Handle nullable deletedAt
        spec = if (ganttTask.deletedAt != null) {
            spec.bind("deletedAt", ganttTask.deletedAt.toOffsetDateTime())
        } else {
            spec.bindNull("deletedAt", OffsetDateTime::class.java)
        }

        val rowsUpdated = spec.fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun softDelete(id: UUID): Boolean {
        val now = Instant.now()
        val sql = """
            UPDATE gantt_tasks
            SET deleted_at = :deletedAt
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("id", id)
            .bind("deletedAt", now.toOffsetDateTime())
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun softDeleteByTodoId(todoId: UUID): Boolean {
        val now = Instant.now()
        val sql = """
            UPDATE gantt_tasks
            SET deleted_at = :deletedAt
            WHERE todo_id = :todoId AND deleted_at IS NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("todoId", todoId)
            .bind("deletedAt", now.toOffsetDateTime())
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun restore(id: UUID): Boolean {
        val sql = """
            UPDATE gantt_tasks
            SET deleted_at = NULL
            WHERE id = :id AND deleted_at IS NOT NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun restoreByTodoId(todoId: UUID): Boolean {
        val sql = """
            UPDATE gantt_tasks
            SET deleted_at = NULL
            WHERE todo_id = :todoId AND deleted_at IS NOT NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("todoId", todoId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun existsByTodoId(todoId: UUID): Boolean {
        val sql = """
            SELECT COUNT(*) as count
            FROM gantt_tasks
            WHERE todo_id = :todoId AND deleted_at IS NULL
        """.trimIndent()

        val count = databaseClient.sql(sql)
            .bind("todoId", todoId)
            .map { row, _ -> row.get("count", java.lang.Long::class.java) ?: 0L }
            .one()
            .awaitSingle()

        return count > 0
    }

    /**
     * R2DBC Row를 GanttTask 도메인 엔티티로 변환합니다.
     *
     * 비즈니스 의미: 데이터베이스 레코드를 비즈니스 로직이 이해할 수 있는 도메인 모델로 변환합니다.
     */
    private fun Row.toGanttTask(): GanttTask {
        return GanttTask(
            id = this.get("id", UUID::class.java)!!,
            todoId = this.get("todo_id", UUID::class.java)!!,
            startDate = this.get("start_date", OffsetDateTime::class.java)!!.toInstant(),
            endDate = this.get("end_date", OffsetDateTime::class.java)!!.toInstant(),
            progress = this.get("progress", Integer::class.java)?.toInt() ?: 0,
            deletedAt = this.get("deleted_at", OffsetDateTime::class.java)?.toInstant()
        )
    }

    /**
     * Instant를 OffsetDateTime으로 변환합니다 (UTC 기준).
     */
    private fun Instant.toOffsetDateTime(): OffsetDateTime {
        return this.atOffset(ZoneOffset.UTC)
    }
}
