# Testing Strategy and Patterns

## Test Framework Stack

### Core Testing Libraries
- **JUnit 5**: Main test framework (JUnit Platform)
- **kotlin-test-junit5**: Kotlin-specific test utilities
- **reactor-test**: For testing reactive streams (`StepVerifier`)
- **kotlinx-coroutines-test**: For testing coroutines (`runTest`)
- **Testcontainers**: For integration tests with real PostgreSQL

## Integration Testing with Testcontainers

### Base Test Class Pattern

**Location**: `src/test/kotlin/io/hcode/task_core/integration/IntegrationTestBase.kt`

**Purpose**: Shared PostgreSQL container for all integration tests

**Pattern**:
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class IntegrationTestBase {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("task-core")
            withUsername("task_app")
            withPassword("test_password")
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { 
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
```

### Key Features
- **Shared Container**: All tests use same PostgreSQL instance (faster execution)
- **Automatic Cleanup**: Testcontainers manages container lifecycle
- **Dynamic Configuration**: R2DBC and JDBC URLs configured from container
- **Liquibase Execution**: Migrations run automatically before tests
- **Profile**: `test` profile activated by default

## Testing Reactive Code

### Testing Mono/Flux with StepVerifier

**Pattern for Mono**:
```kotlin
@Test
fun `should find todo by id`() {
    val result = todoQueryService.findById(todoId)
    
    StepVerifier.create(result)
        .assertNext { todo ->
            assertEquals(todoId, todo.id)
            assertEquals("Test Todo", todo.title)
        }
        .verifyComplete()
}
```

**Pattern for Flux**:
```kotlin
@Test
fun `should find all todos`() {
    val result = todoQueryService.findAll()
    
    StepVerifier.create(result)
        .expectNextCount(3)
        .verifyComplete()
}
```

### Testing Coroutines

**Pattern with runTest**:
```kotlin
@Test
fun `should create todo`() = runTest {
    val request = CreateTodoRequest(
        title = "New Todo",
        description = "Description"
    )
    
    val result = todoCommandService.create(request).await()
    
    assertNotNull(result.id)
    assertEquals("New Todo", result.title)
}
```

## Testing REST Endpoints with WebTestClient

### Injecting WebTestClient
```kotlin
@Autowired
lateinit var webTestClient: WebTestClient
```

### Testing GET Requests
```kotlin
@Test
fun `should get all todos`() {
    webTestClient.get()
        .uri("/api/todos")
        .exchange()
        .expectStatus().isOk
        .expectBodyList<TodoResponse>()
        .hasSize(3)
}
```

### Testing POST Requests
```kotlin
@Test
fun `should create todo`() {
    val request = CreateTodoRequest(
        title = "New Todo",
        description = "Test"
    )
    
    webTestClient.post()
        .uri("/api/todos")
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated
        .expectBody<TodoResponse>()
        .consumeWith { response ->
            assertNotNull(response.responseBody?.id)
        }
}
```

### Testing PUT Requests
```kotlin
@Test
fun `should update todo`() {
    val request = UpdateTodoRequest(
        title = "Updated Title",
        status = "DONE"
    )
    
    webTestClient.put()
        .uri("/api/todos/{id}", todoId)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectBody<TodoResponse>()
        .consumeWith { response ->
            assertEquals("Updated Title", response.responseBody?.title)
        }
}
```

### Testing DELETE Requests
```kotlin
@Test
fun `should delete todo`() {
    webTestClient.delete()
        .uri("/api/todos/{id}", todoId)
        .exchange()
        .expectStatus().isNoContent
        
    // Verify soft delete
    webTestClient.get()
        .uri("/api/todos/{id}", todoId)
        .exchange()
        .expectStatus().isNotFound
}
```

## Testing SSE Endpoints

### Testing Server-Sent Events
```kotlin
@Test
fun `should receive SSE events`() {
    val events = webTestClient.get()
        .uri("/sse")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isOk
        .returnResult<ServerSentEvent<String>>()
        .responseBody
    
    StepVerifier.create(events.take(3))
        .expectNextMatches { event -> 
            event.event() == "heartbeat" 
        }
        .thenCancel()
        .verify()
}
```

## Test Organization

### Naming Conventions
- Test class: `{ClassName}IntegrationTests` or `{ClassName}Tests`
- Test method: Use backticks for descriptive names
  - `` `should create todo when valid request`() ``
  - `` `should return 404 when todo not found`() ``

### Test Structure (AAA Pattern)
```kotlin
@Test
fun `should do something`() {
    // Arrange: Setup test data
    val request = CreateTodoRequest(...)
    
    // Act: Execute the operation
    val result = service.create(request)
    
    // Assert: Verify the outcome
    StepVerifier.create(result)
        .assertNext { /* assertions */ }
        .verifyComplete()
}
```

## Gradle Test Configuration

### Test Execution Settings
```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = 1  // Sequential execution for Testcontainers
    
    // Rancher Desktop Docker configuration
    environment("DOCKER_HOST", "unix:///Users/hcode/.rd/docker.sock")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    environment("TESTCONTAINERS_CHECKS_DISABLE", "true")
    
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}
```

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests "TodoControllerIntegrationTests"
```

### Specific Test Method
```bash
./gradlew test --tests "TodoControllerIntegrationTests.should create todo when valid request"
```

### View Test Report
```bash
open build/reports/tests/test/index.html  # macOS
```

## Common Pitfalls and Solutions

### 1. Testcontainers Connection Issues
**Problem**: Tests fail with "Cannot connect to Docker daemon"
**Solution**: Check DOCKER_HOST environment variable points to correct socket

### 2. Tests Pass but Logic Fails
**Problem**: Reactive code continues after test completes
**Solution**: Always use `StepVerifier.verify()` or `.await()` in coroutines

### 3. Database State Between Tests
**Problem**: Tests interfere with each other
**Solution**: Use `@Transactional` with rollback or clean database in `@AfterEach`

### 4. R2DBC Connection Pool Exhausted
**Problem**: Tests hang waiting for connections
**Solution**: Ensure proper cleanup, check for leaked connections

## TDD Workflow

### Red-Green-Refactor Cycle

1. **Red**: Write failing test first
```kotlin
@Test
fun `should create todo with default status`() {
    val request = CreateTodoRequest(title = "Test")
    
    StepVerifier.create(todoCommandService.create(request))
        .assertNext { todo ->
            assertEquals(TaskStatus.TODO, todo.status)  // Will fail initially
        }
        .verifyComplete()
}
```

2. **Green**: Implement minimum code to pass
```kotlin
fun create(request: CreateTodoRequest): Mono<Todo> {
    val todo = Todo(
        id = UUID.randomUUID(),
        title = request.title,
        status = TaskStatus.TODO,  // Add this to pass test
        // ... other fields
    )
    return todoStorePort.save(todo)
}
```

3. **Refactor**: Improve code while keeping tests green
```kotlin
fun create(request: CreateTodoRequest): Mono<Todo> {
    return Mono.just(request.toDomainModel())
        .flatMap(todoStorePort::save)
        .flatMap { todo ->
            eventPublishPort.publish(DomainEvent.TodoCreated(todo.id))
                .thenReturn(todo)
        }
}
```

## Test Coverage Goals

- **Unit Tests**: Service layer (business logic)
- **Integration Tests**: Controller → Service → Repository flow
- **Contract Tests**: DTO validation, API contracts
- **Performance Tests**: Reactive stream backpressure handling

**Target**: >80% code coverage for service and adapter layers
