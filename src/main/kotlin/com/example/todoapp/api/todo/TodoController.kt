package com.example.todoapp.api.todo

import com.example.todoapp.api.todo.dto.*
import com.example.todoapp.config.AppProperties
import com.example.todoapp.domain.todo.TodoPriority
import com.example.todoapp.domain.todo.TodoStatus
import com.example.todoapp.security.AuthUser
import com.example.todoapp.service.todo.TodoService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/todos")
class TodoController(
    private val todoService: TodoService,
    private val appProperties: AppProperties
) {
    @GetMapping
    fun listTodos(
        @AuthenticationPrincipal user: AuthUser,
        @RequestParam(required = false) status: TodoStatus?,
        @RequestParam(required = false) priority: TodoPriority?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dueDate: LocalDate?,
        pageable: Pageable
    ): TodoListResponse {
        val maxSize = appProperties.pagination.maxSize
        val safeSize = pageable.pageSize.coerceIn(1, maxSize)
        val safePageable = PageRequest.of(pageable.pageNumber, safeSize, pageable.sort)

        return todoService.listTodos(user.id, status, priority, dueDate, safePageable)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTodo(
        @AuthenticationPrincipal user: AuthUser,
        @RequestBody req: TodoCreateRequest
    ): TodoResponse = todoService.createTodo(user.id, req)

    @GetMapping("/{id}")
    fun getTodo(@AuthenticationPrincipal user: AuthUser, @PathVariable id: UUID): TodoResponse =
        todoService.getTodo(user.id, id)

    @PutMapping("/{id}")
    fun updateTodo(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable id: UUID,
        @RequestBody req: TodoUpdateRequest
    ): TodoResponse = todoService.updateTodo(user.id, id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTodo(@AuthenticationPrincipal user: AuthUser, @PathVariable id: UUID) {
        todoService.deleteTodo(user.id, id)
    }
}