package com.example.todoapp.cache

import com.example.todoapp.domain.todo.TodoPriority
import com.example.todoapp.domain.todo.TodoStatus
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.UUID
import java.util.zip.CRC32

object CacheKeys {

    // ---- Todo list caching ----
    // version key for per-user list invalidation
    @JvmStatic
    fun todosVersion(userId: UUID): String = "todos:version:$userId"

    // list cache key: todos:{userId}:{version}:{queryHash}
    @JvmStatic
    fun todos(userId: UUID, version: Long, queryHash: String): String =
        "todos:$userId:$version:$queryHash"

    // query hash: stable for (status, priority, dueDate, pageable)
    @JvmStatic
    fun queryHash(
        status: TodoStatus?,
        priority: TodoPriority?,
        dueDate: LocalDate?,
        pageable: Pageable
    ): String {
        val raw = buildString {
            append("status=").append(status?.name ?: "").append('|')
            append("priority=").append(priority?.name ?: "").append('|')
            append("dueDate=").append(dueDate?.toString() ?: "").append('|')
            append("page=").append(pageable.pageNumber).append('|')
            append("size=").append(pageable.pageSize).append('|')
            append("sort=").append(pageable.sort.toString())
        }

        // More stable than Kotlin's hashCode() across versions/JVMs
        val crc = CRC32()
        crc.update(raw.toByteArray(Charsets.UTF_8))
        return crc.value.toString()
    }

    // ---- Single todo caching ----
    @JvmStatic
    fun todo(userId: UUID, todoId: UUID): String = "todo:$userId:$todoId"

    // ---- Profile caching ----
    @JvmStatic
    fun userProfile(userId: UUID): String = "profile:$userId"
}