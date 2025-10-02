package io.hcode.task_core.adapter.output.persistence

import io.hcode.task_core.domain.model.TaskPriority
import io.hcode.task_core.domain.model.TaskStatus
import io.hcode.task_core.domain.model.Todo
import io.hcode.task_core.domain.port.output.TodoStorePort
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
 * Todo 영속성 어댑터 (Outbound Adapter)
 *
 * 비즈니스 의미: Todo 엔티티의 데이터베이스 저장소 구현체입니다.
 *
 * 기술적 배경:
 * - R2DBC로 비동기 non-blocking 방식으로 데이터베이스 작업을 수행합니다.
 * - 도메인 모델과 데이터베이스 레코드 간의 매핑을 담당합니다.
 * - 소프트 삭제 패턴을 지원합니다.
 * - Named parameter binding으로 SQL injection을 방지합니다.
 */
@Repository
class TodoStoreAdapter(
    private val databaseClient: DatabaseClient
) : TodoStorePort {

    override suspend fun create(todo: Todo): UUID {
        val sql = """
            INSERT INTO todos (id, title, description, status, priority, created_at, updated_at, deleted_at)
            VALUES (:id, :title, :description, :status, :priority, :createdAt, :updatedAt, :deletedAt)
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", todo.id)
            .bind("title", todo.title)
            .bind("status", todo.status.name)
            .bind("priority", todo.priority.name)
            .bind("createdAt", todo.createdAt.toOffsetDateTime())
            .bind("updatedAt", todo.updatedAt.toOffsetDateTime())

        // Handle nullable description
        spec = if (todo.description != null) {
            spec.bind("description", todo.description)
        } else {
            spec.bindNull("description", String::class.java)
        }

        // Handle nullable deletedAt
        spec = if (todo.deletedAt != null) {
            spec.bind("deletedAt", todo.deletedAt.toOffsetDateTime())
        } else {
            spec.bindNull("deletedAt", OffsetDateTime::class.java)
        }

        spec.then().awaitFirstOrNull()

        return todo.id
    }

    override suspend fun findByIdActive(id: UUID): Todo? {
        val sql = """
            SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
            FROM todos
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toTodo() }
            .one()
            .awaitFirstOrNull()
    }

    override suspend fun findById(id: UUID, includeDeleted: Boolean): Todo? {
        val sql = if (includeDeleted) {
            """
                SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
                FROM todos
                WHERE id = :id
            """.trimIndent()
        } else {
            """
                SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
                FROM todos
                WHERE id = :id AND deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.toTodo() }
            .one()
            .awaitFirstOrNull()
    }

    override fun findAllActive(): Flow<Todo> {
        val sql = """
            SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
            FROM todos
            WHERE deleted_at IS NULL
        """.trimIndent()

        return databaseClient.sql(sql)
            .map { row, _ -> row.toTodo() }
            .all()
            .asFlow()
    }

    override fun findAll(includeDeleted: Boolean): Flow<Todo> {
        val sql = if (includeDeleted) {
            """
                SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
                FROM todos
            """.trimIndent()
        } else {
            """
                SELECT id, title, description, status, priority, created_at, updated_at, deleted_at
                FROM todos
                WHERE deleted_at IS NULL
            """.trimIndent()
        }

        return databaseClient.sql(sql)
            .map { row, _ -> row.toTodo() }
            .all()
            .asFlow()
    }

    override suspend fun update(todo: Todo): Boolean {
        val sql = """
            UPDATE todos
            SET title = :title,
                description = :description,
                status = :status,
                priority = :priority,
                updated_at = :updatedAt,
                deleted_at = :deletedAt
            WHERE id = :id
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("id", todo.id)
            .bind("title", todo.title)
            .bind("status", todo.status.name)
            .bind("priority", todo.priority.name)
            .bind("updatedAt", todo.updatedAt.toOffsetDateTime())

        // Handle nullable description
        spec = if (todo.description != null) {
            spec.bind("description", todo.description)
        } else {
            spec.bindNull("description", String::class.java)
        }

        // Handle nullable deletedAt
        spec = if (todo.deletedAt != null) {
            spec.bind("deletedAt", todo.deletedAt.toOffsetDateTime())
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
            UPDATE todos
            SET deleted_at = :deletedAt,
                updated_at = :updatedAt
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("id", id)
            .bind("deletedAt", now.toOffsetDateTime())
            .bind("updatedAt", now.toOffsetDateTime())
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun restore(id: UUID): Boolean {
        val now = Instant.now()
        val sql = """
            UPDATE todos
            SET deleted_at = NULL,
                updated_at = :updatedAt
            WHERE id = :id AND deleted_at IS NOT NULL
        """.trimIndent()

        val rowsUpdated = databaseClient.sql(sql)
            .bind("id", id)
            .bind("updatedAt", now.toOffsetDateTime())
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        return rowsUpdated > 0
    }

    override suspend fun existsById(id: UUID): Boolean {
        val sql = """
            SELECT COUNT(*) as count
            FROM todos
            WHERE id = :id AND deleted_at IS NULL
        """.trimIndent()

        val count = databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> row.get("count", java.lang.Long::class.java) ?: 0L }
            .one()
            .awaitSingle()

        return count > 0
    }

    /**
     * R2DBC Row를 Todo 도메인 엔티티로 변환합니다.
     *
     * 비즈니스 의미: 데이터베이스 레코드를 비즈니스 로직이 이해할 수 있는 도메인 모델로 변환합니다.
     * 기술적 배경: OffsetDateTime -> Instant 변환, String -> Enum 변환을 수행합니다.
     */
    private fun Row.toTodo(): Todo {
        return Todo(
            id = this.get("id", UUID::class.java)!!,
            title = this.get("title", String::class.java)!!,
            description = this.get("description", String::class.java),
            status = TaskStatus.valueOf(this.get("status", String::class.java)!!),
            priority = TaskPriority.valueOf(this.get("priority", String::class.java)!!),
            createdAt = this.get("created_at", OffsetDateTime::class.java)!!.toInstant(),
            updatedAt = this.get("updated_at", OffsetDateTime::class.java)!!.toInstant(),
            deletedAt = this.get("deleted_at", OffsetDateTime::class.java)?.toInstant()
        )
    }

    /**
     * Instant를 OffsetDateTime으로 변환합니다 (UTC 기준).
     *
     * 비즈니스 의미: 도메인 모델의 시간을 데이터베이스 저장 형식으로 변환합니다.
     * 기술적 배경: PostgreSQL의 TIMESTAMP WITH TIME ZONE은 OffsetDateTime으로 매핑됩니다.
     */
    private fun Instant.toOffsetDateTime(): OffsetDateTime {
        return this.atOffset(ZoneOffset.UTC)
    }
}
