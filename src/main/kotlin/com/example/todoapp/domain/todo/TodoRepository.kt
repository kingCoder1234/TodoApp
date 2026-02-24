package com.example.todoapp.domain.todo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface TodoRepository : JpaRepository<Todo, UUID> {

    @Query(
        """
        select t from Todo t
        where t.userId = :userId
          and t.deletedAt is null
          and (:status is null or t.status = :status)
          and (:priority is null or t.priority = :priority)
          and (:dueDate is null or t.dueDate = :dueDate)
        """
    )
    fun findVisibleByUser(
        @Param("userId") userId: UUID,
        @Param("status") status: TodoStatus?,
        @Param("priority") priority: TodoPriority?,
        @Param("dueDate") dueDate: LocalDate?,
        pageable: Pageable
    ): Page<Todo>

    fun findByIdAndDeletedAtIsNull(id: UUID): Todo?
}