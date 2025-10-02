# Hexagonal Architecture Implementation

## Architectural Pattern

This project implements **Hexagonal Architecture** (Ports and Adapters) with strict layering:

### Layer Responsibilities

#### 1. Domain Layer (`domain/`)
**Purpose**: Core business logic, framework-independent

**Components**:
- `model/`: Domain entities
  - `Todo.kt`: Todo item with status and priority
  - `GanttTask.kt`: Gantt chart task with parent-child relationships
  - `KanbanCard.kt`: Kanban board card with column and order
  - `TaskStatus.kt`: Enum (TODO, IN_PROGRESS, DONE)
  - `TaskPriority.kt`: Enum (LOW, MEDIUM, HIGH, CRITICAL)

- `port/input/`: Use case interfaces (what application can do)
  - Command Ports: `TodoCommandPort`, `GanttCommandPort`, `KanbanCommandPort`
  - Query Ports: `TodoQueryPort`, `GanttQueryPort`, `KanbanQueryPort`

- `port/output/`: External dependencies interfaces (what application needs)
  - Store Ports: `TodoStorePort`, `GanttStorePort`, `KanbanStorePort`
  - Event Port: `EventPublishPort`

**Rules**:
- No framework dependencies (no Spring annotations)
- All dependencies point inward (Dependency Inversion Principle)
- Pure Kotlin/Java with domain types (UUID, LocalDateTime, Mono, Flux)

#### 2. Application Layer (`application/service/`)
**Purpose**: Orchestrate domain logic, implement use cases

**Components**:
- Command Services: Handle write operations (create, update, delete)
- Query Services: Handle read operations (findById, findAll)
- Each service implements corresponding input port
- Services depend on output ports (store, event publish)

**Pattern**: CQRS (Command Query Responsibility Segregation)
- Separate services for commands vs queries
- Clear separation of concerns
- Easier to scale and optimize independently

**Example**: `TodoCommandService`
- Implements `TodoCommandPort`
- Depends on `TodoStorePort` and `EventPublishPort`
- Orchestrates: validation → persistence → event publishing

#### 3. Adapter Layer (`adapter/`)

##### Input Adapters (`adapter/input/web/`)
**Purpose**: Receive external requests, translate to domain operations

**Components**:
- Controllers: `TodoController`, `GanttController`, `KanbanController`, `SseController`
- DTOs: Request/Response objects for each domain
- Exception Handlers: `GlobalExceptionHandler` with Problem+JSON

**Flow**: HTTP Request → DTO → Controller → Input Port → Service

##### Output Adapters (`adapter/output/`)
**Purpose**: Implement external dependencies

**Components**:
- Persistence Adapters: Implement `*StorePort` using R2DBC + JOOQ
  - `TodoStoreAdapter`, `GanttStoreAdapter`, `KanbanStoreAdapter`
  - Use `DslContextWrapper` for type-safe reactive queries
  
- Event Adapters: Implement `EventPublishPort`
  - `SseEventPublishAdapter`: Publish domain events via SSE
  - `DomainEvent`: Sealed class with event types

**Flow**: Service → Output Port → Adapter → External System (DB, SSE)

#### 4. Infrastructure Layer (`infrastructure/`)
**Purpose**: Technical concerns, framework configuration

**Components**:
- `config/`: Spring configuration
  - `TransactionConfig`: R2DBC transaction management
  - `LiquibaseConfig`: JDBC DataSource for migrations
  - `SseConfigProperties`: SSE heartbeat configuration
  
- `jooq/`: JOOQ integration
  - `DslContextWrapper`: Bridge JOOQ with R2DBC
  
- `sse/`: SSE utilities
  - `EventIdGenerator`: Generate unique event IDs

## Dependency Flow

```
┌─────────────────────────────────────────┐
│         Input Adapters (Web)            │
│  Controllers → DTOs → Exception Handler │
└────────────┬────────────────────────────┘
             │ depends on
             ▼
┌─────────────────────────────────────────┐
│          Input Ports (Interfaces)       │
│   TodoCommandPort, TodoQueryPort, etc.  │
└────────────┬────────────────────────────┘
             │ implemented by
             ▼
┌─────────────────────────────────────────┐
│      Application Services (Use Cases)   │
│  CommandService, QueryService per domain│
└────────────┬────────────────────────────┘
             │ depends on
             ▼
┌─────────────────────────────────────────┐
│         Output Ports (Interfaces)       │
│  TodoStorePort, EventPublishPort, etc.  │
└────────────┬────────────────────────────┘
             │ implemented by
             ▼
┌─────────────────────────────────────────┐
│        Output Adapters                  │
│  Persistence (R2DBC+JOOQ), Events (SSE) │
└─────────────────────────────────────────┘
```

## Benefits of This Architecture

1. **Testability**: Mock ports for testing services in isolation
2. **Flexibility**: Swap adapters without changing business logic
3. **Independence**: Domain logic not coupled to frameworks
4. **Clarity**: Clear separation of concerns and dependencies
5. **Scalability**: Easy to add new adapters or modify existing ones

## CQRS Pattern Details

### Command Operations (Write)
- Create, Update, Delete operations
- Publish domain events after successful write
- Examples: `createTodo()`, `updateGanttTask()`, `moveKanbanCard()`

### Query Operations (Read)
- Read-only operations, no side effects
- Optimized for retrieval performance
- Examples: `findById()`, `findAll()`, `findByParentId()`

### Separation Benefits
- Different scaling strategies for read vs write
- Optimized database queries per operation type
- Clear audit trail through command operations
- Simpler caching strategies for queries

## SSE Event Architecture

### Event Types (Sealed Class)
```kotlin
sealed class DomainEvent {
    data class TodoCreated(val id: UUID)
    data class TodoUpdated(val id: UUID)
    data class TodoDeleted(val id: UUID)
    // Similar for Gantt and Kanban
}
```

### Event Flow
1. Command service executes operation
2. Service calls `EventPublishPort.publish(event)`
3. `SseEventPublishAdapter` converts to SSE format
4. Connected clients receive real-time updates

### SSE Configuration
- Heartbeat: 15 seconds (configurable via `task.sse.heartbeat-seconds`)
- Event IDs: Monotonically increasing for client resume
- Error handling: Client reconnection with last event ID
