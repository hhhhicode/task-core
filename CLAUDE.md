# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**task-core** is a Spring Boot 3.5.6 reactive backend module for an internal Project Management System, built with Kotlin 1.9.25 and Java 21.

### Technology Stack

- **Web Layer**: Spring WebFlux (fully reactive, non-blocking)
- **Data Access**: Spring Data R2DBC + JOOQ for type-safe queries
- **Database**: PostgreSQL with r2dbc-postgresql driver
- **Migrations**: Liquibase (requires JDBC connection, runs on startup)
- **Concurrency**: Kotlin Coroutines with Reactor integration
- **Real-time**: Server-Sent Events (SSE) for push notifications
- **Testing**: Testcontainers for integration tests with PostgreSQL

## Development Commands

### Build & Run
```bash
./gradlew build                    # Full build with tests
./gradlew clean build              # Clean build
./gradlew bootRun --args='--spring.profiles.active=local'  # Run locally (port 28082)
./gradlew compileKotlin            # Compile main code only
```

### Testing
```bash
./gradlew test                                          # Run all tests
./gradlew test --tests "ClassName"                      # Run specific test class
./gradlew test --tests "ClassName.methodName"           # Run specific test method
open build/reports/tests/test/index.html               # View test report (macOS)
```

### JOOQ Code Generation
```bash
./gradlew generateJooq             # Generate JOOQ classes from database schema
```

**Important**: JOOQ generation requires a running PostgreSQL database at `localhost:5432/task-core` with credentials `task_app:Hjh761943!`. The generated code is placed in `build/generated-src/jooq/main/`.

### Database Operations

Liquibase migrations run automatically on application startup. To add new migrations:
1. Create a new changeset file in `src/main/resources/db/changelog/`
2. Add it to `db.changelog-master.yaml`
3. Run the application to apply migrations
4. Run `./gradlew generateJooq` to regenerate JOOQ classes

**Never modify existing changelog files** - always create new ones for schema changes.

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

The codebase follows hexagonal architecture with clear separation of concerns:

```
domain/
  model/           # Domain entities (Todo, GanttTask, KanbanCard)
  port/
    input/         # Use case interfaces (Command/Query ports)
    output/        # Repository/external service interfaces

application/
  service/         # Use case implementations (connects input ports to output ports)

adapter/
  input/web/       # REST controllers, DTOs, exception handlers
  output/
    persistence/   # Repository implementations using R2DBC + JOOQ
    event/         # SSE event publishing

infrastructure/
  config/          # Spring configuration (transaction, SSE, Liquibase)
  jooq/            # JOOQ integration with R2DBC
  sse/             # SSE support utilities
```

**Key Flow**: Controller → Input Port → Service (Application) → Output Port → Adapter

### Reactive Programming Model

This application uses a **fully reactive, non-blocking architecture**:

- **Mono<T>**: Single value (or empty) - use for operations returning 0 or 1 result
- **Flux<T>**: Stream of values - use for operations returning 0 to N results
- **Kotlin Coroutines**: Use `suspend` functions with `Flow<T>` for coroutine-based reactive code
- **Critical**: Never use blocking operations in handlers, services, or repositories

**Mixing Reactor and Coroutines**:
- Use `.asFlow()` to convert `Flux<T>` to `Flow<T>`
- Use `mono { }` or `flux { }` to create publishers from coroutine code
- Use `.awaitSingle()` or `.awaitFirst()` to await Mono/Flux in suspend functions

### JOOQ + R2DBC Integration

**DslContextWrapper Pattern**: This project uses a custom `DslContextWrapper` to integrate JOOQ's type-safe SQL generation with R2DBC's reactive execution.

**Usage Pattern**:
```kotlin
// For multiple results (Flux)
fun findAll(): Flux<Todo> = dslWrapper.withDSLContextMany { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.DELETED_AT.isNull)
        .fetchPublisher()
        .map { record -> record.toTodo() }
}

// For single result (Mono)
fun findById(id: UUID): Mono<Todo> = dslWrapper.withDSLContext { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.ID.eq(id))
        .fetchPublisher()
        .map { record -> record.toTodo() }
}

// For coroutines (Flow)
fun findAllAsFlow(): Flow<Todo> = dslWrapper.withDSLContextManyAsFlow { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .fetchPublisher()
        .map { record -> record.toTodo() }
}

// For coroutines (suspend)
suspend fun findByIdAwait(id: UUID): Todo = dslWrapper.withDSLContextAwait { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.ID.eq(id))
        .fetchPublisher()
        .map { record -> record.toTodo() }
}
```

**Key Points**:
- `dsl.fetchPublisher()` returns a reactive Publisher
- Connection management is handled automatically by `DatabaseClient.inConnection()`
- JOOQ provides compile-time type safety for SQL queries
- R2DBC provides reactive, non-blocking execution

### Database Architecture

**Dual Driver Setup**:
- **r2dbc-postgresql**: Reactive database access for application runtime
- **postgresql (JDBC)**: Required by Liquibase for migrations only

**Why both?**:
- R2DBC doesn't support schema migrations (no DDL transaction support)
- Liquibase requires JDBC for reliable migration execution
- At runtime, only R2DBC connections are used for reactive query execution

### Code Organization & Conventions

**Package Structure**:
- Base package: `io.hcode.task_core`
- Uses `snake_case` for package names (matching Kotlin file/module naming)
- Each domain concept (Todo, Gantt, Kanban) has its own set of ports, services, and adapters

**Naming Conventions**:
- Services are split into `CommandService` (write operations) and `QueryService` (read operations)
- Ports are named with `Port` suffix: `TodoCommandPort`, `TodoQueryPort`, `TodoStorePort`
- Adapters are named with `Adapter` suffix: `TodoStoreAdapter`, `SseEventPublishAdapter`

**Testing Conventions**:
- Test class naming: `{ClassName}Tests` (e.g., `TodoControllerIntegrationTests`)
- Integration tests extend `IntegrationTestBase` for Testcontainers setup
- Use `@SpringBootTest` with `RANDOM_PORT` for full integration tests
- Use `WebTestClient` for testing reactive HTTP endpoints
- Use `reactor-test`'s `StepVerifier` for testing reactive streams
- Use `kotlinx-coroutines-test`'s `runTest` for testing coroutines

### Spring Profiles

- **local**: Development profile (port 28082, local PostgreSQL)
- **test**: Integration test profile (Testcontainers PostgreSQL)
- **main**: Production profile

Configuration files: `application-{profile}.yml`

### Server-Sent Events (SSE)

The application supports SSE for real-time push notifications:

- `SseController` provides `/sse` endpoint for client connections
- `EventPublishPort` and `SseEventPublishAdapter` handle event publishing
- `DomainEvent` sealed class defines event types (TodoCreated, TodoUpdated, etc.)
- Heartbeat mechanism prevents proxy/load balancer timeouts (configurable via `task.sse.heartbeat-seconds`)

SSE events are published when domain operations complete (create, update, delete, move).

## Key Constraints

1. **Reactive Constraint**: All database and I/O operations must be non-blocking. Never use blocking JDBC, Thread.sleep(), or blocking I/O in reactive handlers.

2. **Migration Constraint**: Database migrations are append-only; existing changelogs are immutable. Always create new changelog files for schema changes.

3. **JOOQ Regeneration**: After schema changes, you must regenerate JOOQ classes with `./gradlew generateJooq` before the application can compile.

4. **Coroutines + Reactor**: When mixing coroutines and Reactor, use appropriate adapters (`mono {}`, `flux {}`, `.asFlow()`, `.awaitSingle()`) to bridge the two paradigms.

5. **Testing Reactive Code**: Always use appropriate test utilities (`StepVerifier`, `runTest`) for reactive/coroutine code. Regular assertions may pass even when async code fails.

6. **Comment Convention**: Every field, variable, or DTO property should have a comment explaining its business intent, not just its literal function.

## Local Development Requirements

- **Java 21** (toolchain configured in build.gradle.kts)
- **PostgreSQL 15+** running at `localhost:5432`
- **Database**: `task-core` with user `task_app:Hjh761943!`
- **Rancher Desktop or Docker** for Testcontainers (macOS users: Docker socket at `~/.rd/docker.sock`)

## Integration Testing with Testcontainers

Tests use Testcontainers to spin up PostgreSQL in Docker:

```kotlin
class MyIntegrationTest : IntegrationTestBase() {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should do something`() {
        webTestClient.get().uri("/api/todos")
            .exchange()
            .expectStatus().isOk
    }
}
```

**Key Points**:
- `IntegrationTestBase` provides shared PostgreSQL container (faster test execution)
- Testcontainers manages container lifecycle automatically
- Liquibase migrations run automatically before tests
- Tests run sequentially (`maxParallelForks = 1`) to avoid connection conflicts
- JDBC and R2DBC URLs are dynamically configured to use the same container port
