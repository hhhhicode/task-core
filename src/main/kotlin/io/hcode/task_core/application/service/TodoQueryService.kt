package io.hcode.task_core.application.service

import io.hcode.task_core.domain.port.input.TodoQueryPort
import io.hcode.task_core.domain.port.output.TodoStorePort
import io.hcode.task_core.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Todo 조회 서비스 (CQRS - Query Side)
 *
 * 비즈니스 의미: Todo 작업에 대한 모든 읽기 연산을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class TodoQueryService(
    private val todoStorePort: TodoStorePort
) : TodoQueryPort {

    override suspend fun getTodoById(id: UUID, includeDeleted: Boolean): Todo? {
        return todoStorePort.findById(id, includeDeleted)
    }

    override fun getAllTodos(includeDeleted: Boolean): Flow<Todo> {
        return todoStorePort.findAll(includeDeleted)
    }
}
