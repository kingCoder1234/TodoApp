package com.example.todoapp.service.todo

import com.example.todoapp.api.todo.dto.TodoCreateRequest
import com.example.todoapp.api.todo.dto.TodoListResponse
import com.example.todoapp.api.todo.dto.TodoResponse
import com.example.todoapp.api.todo.dto.TodoUpdateRequest
import com.example.todoapp.domain.todo.Todo
import com.example.todoapp.domain.todo.TodoPriority
import com.example.todoapp.domain.todo.TodoRepository
import com.example.todoapp.domain.todo.TodoStatus
import com.example.todoapp.exception.ForbiddenException
import com.example.todoapp.exception.NotFoundException
import com.example.todoapp.logging.logger
import java.time.LocalDate
import java.util.UUID
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoService(
        private val todoRepository: TodoRepository,
        private val redis: StringRedisTemplate
) {
    private val logger = logger()

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ["todoList"],
            key =
                    "T(com.example.todoapp.cache.CacheKeys).todos(#userId, " +
                            "T(com.example.todoapp.cache.CacheKeys).queryHash(#status, #priority, #dueDate, #pageable))"
    )
    fun listTodos(
            userId: UUID,
            status: TodoStatus?,
            priority: TodoPriority?,
            dueDate: LocalDate?,
            pageable: Pageable
    ): TodoListResponse {
        logger.debug(
                "Todo list DB HIT userId={} status={} priority={} dueDate={} page={} size={} sort={}",
                userId,
                status,
                priority,
                dueDate,
                pageable.pageNumber,
                pageable.pageSize,
                pageable.sort
        )

        val page = todoRepository.findVisibleByUser(userId, status, priority, dueDate, pageable)

        return TodoListResponse(
                items = page.content.map(TodoResponse::from),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ["todoSingle"],
            key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun getTodo(userId: UUID, todoId: UUID): TodoResponse {
        logger.debug("Get todo request userId={} todoId={}", userId, todoId)

        val todo =
                todoRepository.findByIdAndDeletedAtIsNull(todoId)
                        ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        return TodoResponse.from(todo)
    }

    @Transactional
    fun createTodo(userId: UUID, req: TodoCreateRequest): TodoResponse {
        val todo =
                Todo(
                        userId = userId,
                        title = req.title.trim(),
                        description = req.description?.trim(),
                        status = req.status ?: TodoStatus.PENDING,
                        priority = req.priority ?: TodoPriority.MEDIUM,
                        dueDate = req.dueDate
                )

        val saved = todoRepository.save(todo)

        // Invalidate all cached list variants for this user (all filters/pages/sorts)
        evictTodoListsForUser(userId)

        logger.info(
                "Todo created userId={} todoId={} status={} priority={}",
                userId,
                saved.id,
                saved.status,
                saved.priority
        )

        return TodoResponse.from(saved)
    }

    @Transactional
    @CacheEvict(
            cacheNames = ["todoSingle"],
            key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun updateTodo(userId: UUID, todoId: UUID, req: TodoUpdateRequest): TodoResponse {
        val todo =
                todoRepository.findByIdAndDeletedAtIsNull(todoId)
                        ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        req.title?.let { todo.title = it.trim() }
        if (req.description != null) todo.description = req.description.trim()
        req.status?.let { todo.status = it }
        req.priority?.let { todo.priority = it }
        todo.dueDate = req.dueDate

        val saved = todoRepository.save(todo)

        // Invalidate all cached list variants for this user
        evictTodoListsForUser(userId)

        logger.info(
                "Todo updated userId={} todoId={} status={} priority={}",
                userId,
                todoId,
                saved.status,
                saved.priority
        )

        return TodoResponse.from(saved)
    }

    @Transactional
    @CacheEvict(
            cacheNames = ["todoSingle"],
            key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun deleteTodo(userId: UUID, todoId: UUID) {
        val todo =
                todoRepository.findByIdAndDeletedAtIsNull(todoId)
                        ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        todo.softDelete()
        todoRepository.save(todo)

        // Invalidate all cached list variants for this user
        evictTodoListsForUser(userId)

        logger.info("Todo deleted userId={} todoId={}", userId, todoId)
    }

    /**
     * Removes all "todoList" cache entries for this user. Needed because list caching has many
     * variants (page/sort/filter).
     */
    private fun evictTodoListsForUser(userId: UUID) {
        val pattern = "todos:$userId:*"
        val keys = redis.keys(pattern)

        if (!keys.isNullOrEmpty()) {
            redis.delete(keys)
            logger.debug("Evicted todoList keys userId={} count={}", userId, keys.size)
        } else {
            logger.debug("No todoList keys to evict userId={}", userId)
        }
    }
}