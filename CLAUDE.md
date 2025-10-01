# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**task-core** is a Spring Boot 3.5.6 reactive backend module for an internal Project Management System, built with Kotlin 1.9.25 and Java 21.

### Technology Stack

- **Web Layer**: Spring WebFlux (fully reactive, non-blocking)
- **Data Access**: Spring Data R2DBC + JOOQ
- **Database**: PostgreSQL with r2dbc-postgresql driver
- **Migrations**: Liquibase (requires JDBC connection, runs on startup)
- **Concurrency**: Kotlin Coroutines with Reactor integration

## Development Commands

### Build & Run
```bash
./gradlew build                    # Full build with tests
./gradlew clean build              # Clean build
./gradlew bootRun                  # Run the application
./gradlew compileKotlin            # Compile main code only
./gradlew compileTestKotlin        # Compile test code only
```

### Testing
```bash
./gradlew test                                          # Run all tests
./gradlew test --tests "ClassName"                      # Run specific test class
./gradlew test --tests "ClassName.methodName"           # Run specific test method
open build/reports/tests/test/index.html               # View test report
```

### Gradle Tasks
```bash
./gradlew tasks                    # List all available tasks
./gradlew dependencies             # Show dependency tree
```

## Architecture & Patterns

### Reactive Programming Model

This application uses a **fully reactive, non-blocking architecture**:

- **For data operations**: Use `Mono<T>` for single values and `Flux<T>` for streams
- **Kotlin style**: Use `suspend` functions with `Flow<T>` for coroutine-based reactive code
- **Critical**: Never use blocking operations in handlers, services, or repositories
- **Thread model**: All I/O operations must be non-blocking to avoid blocking the event loop

### Database Architecture

**Dual Driver Setup**:
- **r2dbc-postgresql**: Reactive database access for application runtime
- **postgresql (JDBC)**: Required by Liquibase for migrations only

**JOOQ Integration**:
- Use JOOQ for type-safe SQL queries
- Integrate with R2DBC for reactive execution
- Generated code from database schema

**Liquibase Migrations**:
- Changelogs location: `src/main/resources/db/changelog/`
- Runs automatically on application startup
- **Never modify existing changelog files** - always create new ones for schema changes

### Code Organization

**Package Structure**:
- Base package: `io.hcode.task_core`
- Uses `snake_case` for package names (matching Kotlin file/module naming)
- Expected to follow domain-driven design or clean architecture as it grows

**Testing Conventions**:
- Test class naming: `{ClassName}Tests`
- Use `@SpringBootTest` for integration tests
- Test packages mirror main package structure
- Use `reactor-test` for testing reactive streams
- Use `kotlinx-coroutines-test` for testing coroutines

**Configuration**:
- `application.properties` for application configuration
- Spring profiles for environment-specific settings

## Key Constraints

1. **Reactive Constraint**: All database and I/O operations must be non-blocking
2. **Migration Constraint**: Database migrations are append-only; existing changelogs are immutable
3. **Coroutines + Reactor**: When mixing coroutines and Reactor, use appropriate adapters (`mono {}`, `flux {}`, `.asFlow()`, `.asPublisher()`)
4. **Testing Reactive Code**: Always use appropriate test utilities (`StepVerifier`, `runTest`) for reactive/coroutine code
