# R2DBC + JOOQ Integration Pattern

## Problem Statement

**Challenge**: Combine JOOQ's type-safe SQL generation with R2DBC's reactive execution

- **JOOQ**: Excellent compile-time type safety, SQL generation, but designed for JDBC
- **R2DBC**: Reactive, non-blocking database access, but lacks type-safe query builder
- **Goal**: Get best of both worlds - JOOQ's type safety + R2DBC's reactive execution

## Solution: DslContextWrapper

### Core Concept

`DslContextWrapper` bridges JOOQ and R2DBC by:
1. Using JOOQ to generate SQL queries (compile-time type checking)
2. Extracting SQL string and parameters from JOOQ
3. Executing via R2DBC `DatabaseClient` for reactive execution
4. Managing connection lifecycle properly

### Implementation Location
`src/main/kotlin/io/hcode/task_core/infrastructure/jooq/DslContextWrapper.kt`

### Key Methods

#### For Multiple Results (Flux)
```kotlin
fun <T> withDSLContextMany(
    block: (DSLContext) -> Publisher<T>
): Flux<T>
```

**Usage in Repository**:
```kotlin
fun findAll(): Flux<Todo> = dslWrapper.withDSLContextMany { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.DELETED_AT.isNull)
        .fetchPublisher()  // Returns Publisher<Record>
        .map { record -> record.toTodo() }  // Map to domain entity
}
```

#### For Single Result (Mono)
```kotlin
fun <T> withDSLContext(
    block: (DSLContext) -> Publisher<T>
): Mono<T>
```

**Usage**:
```kotlin
fun findById(id: UUID): Mono<Todo> = dslWrapper.withDSLContext { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.ID.eq(id))
        .fetchPublisher()
        .map { record -> record.toTodo() }
}
```

#### For Coroutines (Flow)
```kotlin
fun <T> withDSLContextManyAsFlow(
    block: (DSLContext) -> Publisher<T>
): Flow<T>
```

**Usage**:
```kotlin
fun findAllAsFlow(): Flow<Todo> = dslWrapper.withDSLContextManyAsFlow { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .fetchPublisher()
        .map { record -> record.toTodo() }
}
```

#### For Coroutines (Suspend)
```kotlin
suspend fun <T> withDSLContextAwait(
    block: (DSLContext) -> Publisher<T>
): T
```

**Usage**:
```kotlin
suspend fun findByIdAwait(id: UUID): Todo = dslWrapper.withDSLContextAwait { dsl ->
    dsl.select(TODOS.asterisk())
        .from(TODOS)
        .where(TODOS.ID.eq(id))
        .fetchPublisher()
        .map { record -> record.toTodo() }
}
```

## Technical Details

### Connection Management
- Uses `DatabaseClient.inConnection()` for proper resource management
- Connection automatically returned to pool after operation
- Transaction-aware (integrates with `@Transactional`)

### JOOQ Configuration
```kotlin
val dsl = DSL.using(
    connection,
    SQLDialect.POSTGRES,
    Settings().withRenderFormatted(true)
)
```

### Type Safety Flow
1. **Compile Time**: JOOQ validates table/column names, types
2. **Runtime**: JOOQ generates SQL string
3. **Execution**: R2DBC executes reactively
4. **Mapping**: Manual mapping from JOOQ Record to domain entity

### Mapping Pattern
```kotlin
private fun Record.toTodo(): Todo {
    return Todo(
        id = this[TODOS.ID],
        title = this[TODOS.TITLE],
        description = this[TODOS.DESCRIPTION],
        status = TaskStatus.valueOf(this[TODOS.STATUS]),
        priority = TaskPriority.valueOf(this[TODOS.PRIORITY]),
        createdAt = this[TODOS.CREATED_AT],
        updatedAt = this[TODOS.UPDATED_AT],
        deletedAt = this[TODOS.DELETED_AT]
    )
}
```

## JOOQ Code Generation

### Generation Process
```bash
./gradlew generateJooq
```

**What it does**:
1. Connects to database via JDBC
2. Reads schema metadata from Liquibase changelogs
3. Generates type-safe Kotlin classes in `build/generated-src/jooq/main/`
4. Generated classes: Tables, Records, Keys, Indexes

### Generated Code Structure
```
build/generated-src/jooq/main/
└── io.hcode.task_core.infrastructure.jooq.generated/
    ├── DefaultCatalog.kt
    ├── Public.kt                 # Schema
    ├── tables/
    │   ├── Todos.kt              # Type-safe table reference
    │   ├── GanttTasks.kt
    │   └── KanbanCards.kt
    └── tables/records/
        ├── TodosRecord.kt        # Type-safe record class
        ├── GanttTasksRecord.kt
        └── KanbanCardsRecord.kt
```

### Usage of Generated Code
```kotlin
import io.hcode.task_core.infrastructure.jooq.generated.tables.Todos.TODOS

// Type-safe column references
dsl.select(
    TODOS.ID,           // UUID column
    TODOS.TITLE,        // String column
    TODOS.STATUS        // String column (enum)
)
```

## Benefits

1. **Type Safety**: Compile-time checking of SQL queries
2. **Refactoring**: Rename column in DB → Liquibase → regenerate JOOQ → compile errors guide changes
3. **Performance**: Reactive execution, non-blocking I/O
4. **Readability**: SQL-like syntax in Kotlin
5. **Maintenance**: Schema changes caught at compile time

## Important Notes

### After Schema Changes
1. Update Liquibase changeset
2. Run application (Liquibase applies migration)
3. Run `./gradlew generateJooq` to regenerate classes
4. Update repository mappers if column types changed

### Transaction Boundaries
- `@Transactional` works with R2DBC
- Connection management integrated with Spring's transaction manager
- See `TransactionConfig.kt` for configuration

### Performance Considerations
- JOOQ code generation is incremental (only regenerates if schema changed)
- Connection pool managed by R2DBC (configured in application YAML)
- Reactive execution prevents thread blocking

## Dual Driver Architecture

### Why Two Database Drivers?

**R2DBC (r2dbc-postgresql)**:
- For reactive runtime operations
- Non-blocking I/O
- Used by repositories via DslContextWrapper
- Configured via `spring.r2dbc.*`

**JDBC (postgresql)**:
- For Liquibase migrations only
- Liquibase doesn't support R2DBC
- Used only at application startup
- Configured via `spring.datasource.*`

### Configuration Example
```yaml
# R2DBC for reactive operations
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/task-core
    username: task_app
    password: Hjh761943!

# JDBC for Liquibase migrations
  datasource:
    url: jdbc:postgresql://localhost:5432/task-core
    username: task_app
    password: Hjh761943!
    driver-class-name: org.postgresql.Driver
```

**Both point to same database**, just different drivers for different purposes.
