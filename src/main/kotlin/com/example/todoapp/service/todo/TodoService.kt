package com.example.todoapp.service.todo

import com.example.todoapp.api.todo.dto.TodoCreateRequest
import com.example.todoapp.api.todo.dto.TodoListResponse
import com.example.todoapp.api.todo.dto.TodoResponse
import com.example.todoapp.api.todo.dto.TodoUpdateRequest
import com.example.todoapp.cache.CacheKeys
import com.example.todoapp.domain.todo.Todo
import com.example.todoapp.domain.todo.TodoPriority
import com.example.todoapp.domain.todo.TodoRepository
import com.example.todoapp.domain.todo.TodoStatus
import com.example.todoapp.exception.ForbiddenException
import com.example.todoapp.exception.NotFoundException
import com.example.todoapp.logging.logger
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TodoService(
    private val todoRepository: TodoRepository,
    private val cacheManager: CacheManager,
    private val redis: StringRedisTemplate
) {
    private val logger = logger()

    @Transactional(readOnly = true)
    fun listTodos(
        userId: UUID,
        status: TodoStatus?,
        priority: TodoPriority?,
        dueDate: LocalDate?,
        pageable: Pageable
    ): TodoListResponse {
        val version = currentListVersion(userId)
        val queryHash = CacheKeys.queryHash(status, priority, dueDate, pageable)
        val cacheKey = CacheKeys.todos(userId, version, queryHash)

        val cache = cacheManager.getCache("todoList")
        val cached = cache?.get(cacheKey, TodoListResponse::class.java)
        if (cached != null) {
            logger.info("Todo list cache HIT userId={} version={} key={}", userId, version, cacheKey)
            return cached
        }

        logger.info(
            "Todo list cache MISS userId={} version={} status={} priority={} dueDate={} page={} size={} sort={}",
            userId, version, status, priority, dueDate, pageable.pageNumber, pageable.pageSize, pageable.sort
        )

        val page = todoRepository.findVisibleByUser(userId, status, priority, dueDate, pageable)
        val response = TodoListResponse(
            items = page.content.map(TodoResponse::from),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )

        cache?.put(cacheKey, response)
        return response
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["todoSingle"],
        key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun getTodo(userId: UUID, todoId: UUID): TodoResponse {
        logger.info("Get todo request userId={} todoId={}", userId, todoId)

        val todo = todoRepository.findByIdAndDeletedAtIsNull(todoId)
            ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        return TodoResponse.from(todo)
    }

    @Transactional
    fun createTodo(userId: UUID, req: TodoCreateRequest): TodoResponse {
        val todo = Todo(
            userId = userId,
            title = req.title.trim(),
            description = req.description?.trim(),
            status = req.status ?: TodoStatus.PENDING,
            priority = req.priority ?: TodoPriority.MEDIUM,
            dueDate = req.dueDate
        )

        val saved = todoRepository.save(todo)

        bumpListVersion(userId)
        logger.info(
            "Todo created userId={} todoId={} status={} priority={}",
            userId, saved.id, saved.status, saved.priority
        )

        return TodoResponse.from(saved)
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["todoSingle"],
        key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun updateTodo(userId: UUID, todoId: UUID, req: TodoUpdateRequest): TodoResponse {
        val todo = todoRepository.findByIdAndDeletedAtIsNull(todoId)
            ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        req.title?.let { todo.title = it.trim() }
        if (req.description != null) todo.description = req.description.trim()
        req.status?.let { todo.status = it }
        req.priority?.let { todo.priority = it }
        todo.dueDate = req.dueDate

        val saved = todoRepository.save(todo)

        bumpListVersion(userId)
        logger.info(
            "Todo updated userId={} todoId={} status={} priority={}",
            userId, todoId, saved.status, saved.priority
        )

        return TodoResponse.from(saved)
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["todoSingle"],
        key = "T(com.example.todoapp.cache.CacheKeys).todo(#userId, #todoId)"
    )
    fun deleteTodo(userId: UUID, todoId: UUID) {
        val todo = todoRepository.findByIdAndDeletedAtIsNull(todoId)
            ?: throw NotFoundException("Todo not found")

        if (todo.userId != userId) throw ForbiddenException("Forbidden")

        todo.softDelete()
        todoRepository.save(todo)

        bumpListVersion(userId)
        logger.info("Todo deleted userId={} todoId={}", userId, todoId)
    }

    private fun currentListVersion(userId: UUID): Long {
        val key = CacheKeys.todosVersion(userId) // e.g. "todos:version:<userId>"
        val v = redis.opsForValue().get(key) ?: return 0L
        return v.toLongOrNull() ?: 0L
    }

    private fun bumpListVersion(userId: UUID) {
        val key = CacheKeys.todosVersion(userId)
        val next = redis.opsForValue().increment(key) ?: 0L
        logger.info("Todo list version bumped userId={} to={}", userId, next)
    }
}