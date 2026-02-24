
package com.example.todoapp.api.todo.dto

data class TodoListResponse(
    val items: List<TodoResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)