package io.hcode.task_core.adapter.output.persistence

import io.hcode.task_core.domain.model.KanbanCard
import io.hcode.task_core.domain.port.output.KanbanStorePort
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
 * KanbanCard 영속성 어댑터 (Outbound Adapter)
 *
 * 비즈니스 의미: KanbanCard 엔티티의 데이터베이스 저장소 구현체입니다.
 *
 * 기술적 배경:
 * - R2DBC로 비동기 non-blocking 방식으로 데이터베이스 작업을 수행합니다.
 * - 소프트 삭제 패턴을 지원합니다.
 * - Todo와 1:1 관계를 유지합니다 (todo_id는 유니크).
 * - 칸반 보드 내 위치 관리 (column_id + position 조합).
 */
@Repository
class KanbanStoreAdapter(
    private val databaseClient: DatabaseClient
) : KanbanStorePort {

    override suspend fun create(kanbanCard: KanbanCard): UUID {
        val sql = """
            INSERT INTO kanban_cards (id, todo_id, column_id, position, deleted_at)
            VALUES (:id, :todoId, :columnId, :position, :deletedAt)
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", kanbanCard.id)
            .bind("todoId", kanbanCard.todoId)
            .bind("columnId", kanbanCard.columnId)
            .bind("position", kanbanCard.position)

        // Handle nullable deletedAt
        spec = if (kanbanCard.deletedAt != null) {
            spec.bind("deletedAt", kanbanCard.deletedAt.toOffsetDateTime())
        } else {
            spec.bindNull("deletedAt", OffsetDateTime::class.java)
        }

        spec.then().awaitFirstOrNull()

        return kanbanCard.id
    }

    override suspend fun findByIdActive(id: UUID): KanbanCard? {
        val sql = """
            SELECT id, todo_id, column_id, position, deleted_at
            FROM kanban_cards
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toKanbanCard() }
            .one()
            .awaitFirstOrNull()
    }

    override suspend fun findById(id: UUID, includeDeleted: Boolean): KanbanCard? {
        val sql = if (includeDeleted) {
            """
                SELECT id, todo_id, column_id, position, deleted_at
                FROM kanban_cards
                WHERE id = :id
            """.trimIndent()
        } else {
            """
                SELECT id, todo_id, column_id, position, deleted_at
                FROM kanban_cards
                WHERE id = :id AND deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toKanbanCard() }
            .one()
            .awaitFirstOrNull()
    }

    override suspend fun findByTodoIdActive(todoId: UUID): KanbanCard? {
        val sql = """
            SELECT id, todo_id, column_id, position, deleted_at
            FROM kanban_cards
            WHERE todo_id = :todoId AND deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("todoId", todoId)
            .map { row, _ -> row.toKanbanCard() }
            .one()
            .awaitFirstOrNull()
    }

    override fun findByColumnIdActive(columnId: String): Flow<KanbanCard> {
        val sql = """
            SELECT id, todo_id, column_id, position, deleted_at
            FROM kanban_cards
            WHERE column_id = :columnId AND deleted_at IS NULL
            ORDER BY position ASC
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("columnId", columnId)
            .map { row, _ -> row.toKanbanCard() }
            .all()
            .asFlow()
    }

    override fun findByColumnIdsActive(columnIds: Collection<String>): Flow<KanbanCard> {
        if (columnIds.isEmpty()) {
            return kotlinx.coroutines.flow.emptyFlow()
        }

        val placeholders = columnIds.indices.joinToString(",") { ":columnId$it" }
        val sql = """
            SELECT id, todo_id, column_id, position, deleted_at
            FROM kanban_cards
            WHERE column_id IN ($placeholders) AND deleted_at IS NULL
            ORDER BY column_id ASC, position ASC
        """.trimIndent()

        var spec = databaseClient.sql(sql)
        columnIds.forEachIndexed { index, columnId ->
            spec = spec.bind("columnId$index", columnId)
        }

        return spec.map { row, _ -> row.toKanbanCard() }
            .all()
            .asFlow()
    }

    override fun findAllActive(): Flow<KanbanCard> {
        val sql = """
            SELECT id, todo_id, column_id, position, deleted_at
            FROM kanban_cards
            WHERE deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .map { row, _ -> row.toKanbanCard() }
            .all()
            .asFlow()
    }

    override fun findAll(includeDeleted: Boolean): Flow<KanbanCard> {
        val sql = if (includeDeleted) {
            """
                SELECT id, todo_id, column_id, position, deleted_at
                FROM kanban_cards
            """.trimIndent()
        } else {
            """
                SELECT id, todo_id, column_id, position, deleted_at
                FROM kanban_cards
                WHERE deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .map { row, _ -> row.toKanbanCard() }
            .all()
            .asFlow()
    }

    override suspend fun update(kanbanCard: KanbanCard): Boolean {
        val sql = """
            UPDATE kanban_cards
            SET column_id = :columnId,
                position = :position,
                deleted_at = :deletedAt
            WHERE id = :id
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", kanbanCard.id)
            .bind("columnId", kanbanCard.columnId)
            .bind("position", kanbanCard.position)

        // Handle nullable deletedAt
        spec = if (kanbanCard.deletedAt != null) {
            spec.bind("deletedAt", kanbanCard.deletedAt.toOffsetDateTime())
        } else {
            spec.bindNull("deletedAt", OffsetDateTime::class.java)
        }

        val rowsUpdated = spec.fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun shiftPositions(columnId: String, fromPosition: Int, shift: Int): Int {
        val sql = """
            UPDATE kanban_cards
            SET position = position + :shift
            WHERE column_id = :columnId
              AND position >= :fromPosition
              AND deleted_at IS NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("columnId", columnId)
            .bind("fromPosition", fromPosition)
            .bind("shift", shift)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated.toInt()
    }

    override suspend fun softDelete(id: UUID): Boolean {
        val now = Instant.now()
        val sql = """
            UPDATE kanban_cards
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
            UPDATE kanban_cards
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
            UPDATE kanban_cards
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
            UPDATE kanban_cards
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
            FROM kanban_cards
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
     * R2DBC Row를 KanbanCard 도메인 엔티티로 변환합니다.
     *
     * 비즈니스 의미: 데이터베이스 레코드를 비즈니스 로직이 이해할 수 있는 도메인 모델로 변환합니다.
     */
    private fun Row.toKanbanCard(): KanbanCard {
        return KanbanCard(
            id = this.get("id", UUID::class.java)!!,
            todoId = this.get("todo_id", UUID::class.java)!!,
            columnId = this.get("column_id", String::class.java)!!,
            position = this.get("position", Integer::class.java)?.toInt() ?: 0,
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
