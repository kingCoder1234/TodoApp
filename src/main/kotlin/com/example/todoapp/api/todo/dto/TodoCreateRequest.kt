package com.example.todoapp.api.todo.dto

import com.example.todoapp.domain.todo.TodoPriority
import com.example.todoapp.domain.todo.TodoStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class TodoCreateRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    val description: String? = null,

    val status: TodoStatus? = null,
    val priority: TodoPriority? = null,

    val dueDate: LocalDate? = null
)