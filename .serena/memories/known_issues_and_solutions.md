# Known Issues and Solutions

## Current Critical Issues

### 1. Missing JDBC DataSource Configuration ⚠️

**Status**: Active - Blocking application startup

**Error**:
```
Parameter 0 of method liquibase in io.hcode.task_core.infrastructure.config.LiquibaseConfig 
required a bean of type 'javax.sql.DataSource' that could not be found.
```

**Root Cause**:
- `LiquibaseConfig.liquibaseDataSource()` method (line 33-40) reads from `spring.datasource.*` properties
- `application-local.yml` only has `spring.r2dbc.*` configuration
- Missing JDBC configuration for Liquibase

**Solution**:
Add to `src/main/resources/application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task-core
    username: task_app
    password: Hjh761943!
    driver-class-name: org.postgresql.Driver
```

**Verification**:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
# Should start successfully on port 28082
```

### 2. Multiple Background Processes Running

**Status**: Active - Resource cleanup needed

**Issue**: 10 bootRun processes running in background, all failing with same error

**Process IDs**:
- 184241, e3d5f5, fc864e, 71e1a8, 6c646c
- 271b46, 7a00ab, 30d426, 34beff, e5fd0f

**Solution**:
```bash
# List processes
ps aux | grep bootRun

# Kill specific process
kill -9 <PID>

# Or kill all Gradle daemon processes
./gradlew --stop

# Verify port is free
lsof -ti :28082
```

## Common Development Issues

### Issue: JOOQ Code Generation Fails

**Symptoms**: 
- Build fails with "cannot find symbol" for JOOQ generated classes
- `build/generated-src/jooq/main/` is empty

**Causes**:
1. PostgreSQL database not running
2. Database `task-core` doesn't exist
3. Wrong credentials in `build.gradle.kts`
4. Liquibase migrations haven't been applied

**Solution**:
```bash
# 1. Check PostgreSQL is running
psql -U task_app -d task-core -c "SELECT 1"

# 2. If database doesn't exist, create it
createdb -U task_app task-core

# 3. Run application once to apply Liquibase migrations
./gradlew bootRun --args='--spring.profiles.active=local'

# 4. Generate JOOQ code
./gradlew generateJooq

# 5. Verify generated files
ls build/generated-src/jooq/main/io/hcode/task_core/infrastructure/jooq/generated/tables/
```

### Issue: Tests Fail with Testcontainers Connection Error

**Symptoms**:
```
Could not find a valid Docker environment
org.testcontainers.dockerclient.DockerClientProviderStrategy
```

**Causes**:
1. Docker/Rancher Desktop not running
2. Wrong Docker socket path
3. Ryuk container causing issues

**Solution**:
```bash
# 1. Start Rancher Desktop
# Verify it's running: docker ps should work

# 2. Check Docker socket exists
ls -l ~/.rd/docker.sock

# 3. Update DOCKER_HOST in build.gradle.kts if needed
environment("DOCKER_HOST", "unix:///Users/hcode/.rd/docker.sock")

# 4. Disable Ryuk (already configured)
environment("TESTCONTAINERS_RYUK_DISABLED", "true")
```

### Issue: R2DBC Connection Pool Exhausted

**Symptoms**:
- Application hangs on database operations
- Logs show "Pool exhausted" or "Timed out waiting for connection"

**Causes**:
1. Not releasing connections properly
2. Too many concurrent requests
3. Connection leak in code
4. Pool size too small

**Solution**:
```yaml
# In application-local.yml, add:
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 20
      max-idle-time: 30m
      max-acquire-time: 3s
```

**Code Review**:
- Ensure all `Mono`/`Flux` are properly subscribed
- Check for blocking operations in reactive code
- Verify `DslContextWrapper` properly releases connections

### Issue: Liquibase Migration Conflicts

**Symptoms**:
- Application fails to start with "Validation Failed" from Liquibase
- Checksum mismatch errors

**Causes**:
1. Modified existing changelog file (forbidden)
2. Database schema out of sync
3. Manual database changes

**Solution**:
```bash
# Option 1: Clear checksums (development only)
# Add to application-local.yml temporarily:
spring:
  liquibase:
    clear-checksums: true

# Run once, then remove the setting

# Option 2: Reset database (development only)
dropdb -U task_app task-core
createdb -U task_app task-core
./gradlew bootRun --args='--spring.profiles.active=local'

# Option 3: Create new changeset for fix
# Never modify existing changelog files!
```

### Issue: SSE Connections Drop Frequently

**Symptoms**:
- Clients disconnect after 30-60 seconds
- No events received after initial connection

**Causes**:
1. Proxy/load balancer timeout
2. No heartbeat mechanism
3. Network issues

**Solution**:
```yaml
# Configure heartbeat in application-local.yml:
task:
  sse:
    heartbeat-seconds: 15  # Send heartbeat every 15s
```

**Client-side**:
```javascript
const eventSource = new EventSource('/sse');
eventSource.addEventListener('heartbeat', () => {
  // Connection still alive
});
```

### Issue: Reactive Code Not Executing

**Symptoms**:
- No database operations happen
- Tests pass but data not persisted

**Cause**: Forgot to subscribe to `Mono`/`Flux`

**Solution**:
```kotlin
// ❌ Wrong: No subscription, nothing happens
val result = todoStorePort.save(todo)

// ✅ Correct: Subscribe via flatMap/map/subscribe
return todoStorePort.save(todo)
    .flatMap { savedTodo ->
        eventPublishPort.publish(DomainEvent.TodoCreated(savedTodo.id))
            .thenReturn(savedTodo)
    }
```

## Deployment Issues

### Issue: Port 28082 Already in Use

**Symptoms**:
```
Port 28082 was already in use
```

**Solution**:
```bash
# Find process using port
lsof -ti :28082

# Kill process
lsof -ti :28082 | xargs kill -9

# Verify port is free
lsof -ti :28082  # Should return nothing

# Restart application
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Issue: Out of Memory During Build

**Symptoms**:
- Build fails with "OutOfMemoryError"
- Gradle daemon crashes

**Solution**:
```bash
# Create/edit gradle.properties
echo "org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m" > gradle.properties

# Stop Gradle daemon and rebuild
./gradlew --stop
./gradlew clean build
```

## Performance Issues

### Slow JOOQ Code Generation

**Symptoms**: `generateJooq` task takes >30 seconds

**Optimization**:
- JOOQ only regenerates if schema changed (incremental generation)
- If you haven't changed schema, generation should be <1s
- Check if Liquibase migrations are being reapplied unnecessarily

**Solution**: Use schema version providers (see JOOQ docs)

### Slow Tests

**Symptoms**: Integration tests take >2 minutes

**Optimizations**:
1. Share Testcontainers instance (already done via `IntegrationTestBase`)
2. Use `@DirtiesContext` sparingly
3. Minimize database operations in tests
4. Use `@Transactional` with rollback for data cleanup

## Best Practices for Prevention

1. **Never modify existing Liquibase changelogs** - always create new ones
2. **Always regenerate JOOQ after schema changes** - `./gradlew generateJooq`
3. **Use StepVerifier for all reactive tests** - ensure proper subscription
4. **Clean up background processes** - `./gradlew --stop` before ending session
5. **Verify application starts** - before committing changes
6. **Run tests before commit** - `./gradlew test`
7. **Check git status** - `git status` before making changes

## Quick Diagnostics

### Health Check Commands
```bash
# Database connectivity
psql -U task_app -d task-core -c "SELECT 1"

# Application health
curl http://localhost:28082/actuator/health

# JOOQ generated code exists
ls build/generated-src/jooq/main/io/hcode/task_core/infrastructure/jooq/generated/

# Port availability
lsof -ti :28082

# Docker available (for tests)
docker ps

# Gradle daemon status
./gradlew --status
```

### Log Analysis
```bash
# View application logs
tail -f build/logs/application.log

# View test logs
cat build/reports/tests/test/index.html

# View Gradle build logs
./gradlew build --info
```
