package io.hcode.task_core.infrastructure.jooq

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.reactivestreams.Publisher
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * jOOQ DSLContext 제공 및 R2DBC 통합 클래스
 *
 * 비즈니스 의미: jOOQ의 타입 안전 쿼리 빌더와 R2DBC의 반응형 실행을 통합합니다.
 *
 * 기술적 배경:
 * - jOOQ를 사용하여 타입 안전한 SQL을 생성합니다.
 * - R2DBC Connection을 통해 반응형으로 쿼리를 실행합니다.
 * - Connection 기반 실행으로 트랜잭션과 Connection Pool을 효율적으로 관리합니다.
 *
 * 사용 패턴 (고급):
 * ```kotlin
 * // 단일 결과 조회
 * val todo: Todo? = withDSLContext { dsl ->
 *     dsl.select(TODOS.asterisk())
 *         .from(TODOS)
 *         .where(TODOS.ID.eq(id))
 *         .fetchPublisher()
 *         .map { record -> record.toTodo() }
 * }
 *
 * // 다중 결과 조회
 * val todos: Flow<Todo> = withDSLContextMany { dsl ->
 *     dsl.select(TODOS.asterisk())
 *         .from(TODOS)
 *         .where(TODOS.DELETED_AT.isNull)
 *         .fetchPublisher()
 *         .map { record -> record.toTodo() }
 * }
 * ```
 *
 * 사용 패턴 (기존 호환):
 * ```kotlin
 * val sql = dsl
 *     .select(TODOS.ID, TODOS.TITLE)
 *     .from(TODOS)
 *     .where(TODOS.DELETED_AT.isNull)
 *     .getSQL()
 *
 * databaseClient.sql(sql).fetch().all()
 * ```
 *
 * @see org.jooq.DSLContext
 * @see org.springframework.r2dbc.core.DatabaseClient
 */
@Repository
class DslContextWrapper(
    private val databaseClient: DatabaseClient
) {

    /**
     * jOOQ 설정
     *
     * - bindOffsetDateTimeType: OffsetDateTime을 TIMESTAMPTZ로 바인딩
     * - bindOffsetTimeType: OffsetTime을 TIMETZ로 바인딩
     */
    private val settings = Settings()
        .withBindOffsetDateTimeType(true)
        .withBindOffsetTimeType(true)

    /**
     * R2DBC Connection에서 jOOQ DSLContext를 생성합니다.
     *
     * 비즈니스 의미: Connection 기반 쿼리 실행을 위한 DSLContext를 제공합니다.
     * 기술적 배경: Connection을 통해 트랜잭션 범위 내에서 안전하게 쿼리를 실행할 수 있습니다.
     */
    private fun Connection.dsl(): DSLContext = DSL.using(this, SQLDialect.POSTGRES, settings)

    /**
     * jOOQ DSLContext 인스턴스 (기존 호환성)
     *
     * SQL 생성 전용으로 사용됩니다 (실제 실행 X).
     * PostgreSQL 방언을 사용하며, 연결 없이 SQL 문자열만 생성합니다.
     *
     * 참고: 새로운 코드에서는 withDSLContext() 또는 withDSLContextMany()를 사용하는 것을 권장합니다.
     */
    val dsl: DSLContext = DSL.using(SQLDialect.POSTGRES, settings)

    /**
     * 다중 결과를 반환하는 jOOQ 쿼리를 실행합니다.
     *
     * 비즈니스 의미: 여러 레코드를 조회하는 쿼리를 반응형으로 실행합니다.
     * 기술적 배경:
     * - R2DBC Connection을 통해 쿼리를 실행합니다.
     * - Flux로 결과를 스트리밍하여 메모리 효율성을 높입니다.
     * - Connection은 자동으로 관리되며, 쿼리 완료 후 반환됩니다.
     *
     * @param block jOOQ DSLContext를 사용하여 Publisher를 생성하는 람다
     * @return 쿼리 결과의 Flux
     */
    fun <T : Any> withDSLContextMany(block: (DSLContext) -> Publisher<T>): Flux<T> =
        databaseClient.inConnectionMany { connection ->
            Flux.from(block(connection.dsl()))
        }

    /**
     * 단일 결과를 반환하는 jOOQ 쿼리를 실행합니다.
     *
     * 비즈니스 의미: 단일 레코드를 조회하거나 집계 쿼리를 실행합니다.
     * 기술적 배경:
     * - R2DBC Connection을 통해 쿼리를 실행합니다.
     * - Mono로 결과를 반환하여 반응형 체인을 유지합니다.
     * - Connection은 자동으로 관리되며, 쿼리 완료 후 반환됩니다.
     *
     * @param block jOOQ DSLContext를 사용하여 Publisher를 생성하는 람다
     * @return 쿼리 결과의 Mono
     */
    fun <T : Any> withDSLContext(block: (DSLContext) -> Publisher<T>): Mono<T> =
        databaseClient.inConnection { connection ->
            Mono.from(block(connection.dsl()))
        }

    /**
     * 다중 결과를 반환하는 jOOQ 쿼리를 Kotlin Flow로 실행합니다 (코루틴 지원).
     *
     * 비즈니스 의미: 코루틴 환경에서 여러 레코드를 조회하는 쿼리를 실행합니다.
     * 기술적 배경: Flux를 Flow로 변환하여 코루틴 기반 코드와 통합합니다.
     *
     * @param block jOOQ DSLContext를 사용하여 Publisher를 생성하는 람다
     * @return 쿼리 결과의 Flow
     */
    fun <T : Any> withDSLContextManyAsFlow(block: (DSLContext) -> Publisher<T>): Flow<T> =
        withDSLContextMany(block).asFlow()

    /**
     * 단일 결과를 반환하는 jOOQ 쿼리를 코루틴으로 실행합니다.
     *
     * 비즈니스 의미: 코루틴 환경에서 단일 레코드를 조회하거나 집계 쿼리를 실행합니다.
     * 기술적 배경: Mono를 suspend 함수로 변환하여 코루틴 기반 코드와 통합합니다.
     *
     * @param block jOOQ DSLContext를 사용하여 Publisher를 생성하는 람다
     * @return 쿼리 결과 (nullable)
     */
    suspend fun <T : Any> withDSLContextAwait(block: (DSLContext) -> Publisher<T>): T =
        withDSLContext(block).awaitSingle()
}
