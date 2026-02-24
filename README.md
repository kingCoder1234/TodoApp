# HELP.md

This project implements a TODO REST API using **Kotlin + Spring Boot + PostgreSQL + Redis**, with **JWT auth**, **rate limiting**, and **Redis caching**.

---

## 0) Quick Start (Local)

### Prereqs
- Java 17+
- PostgreSQL
- Redis

### Run
1. Configure `src/main/resources/application.yml`
2. Start PostgreSQL + Redis
3. Start the app:
    - `./gradlew bootRun`

Swagger UI is available if OpenAPI/Swagger is enabled in the project.

---

## 1) Folder Structure (High Level)

    src/main/kotlin/com/.../
    config/
    api/
    auth/
    todo/
    domain/
    user/
    todo/
    token/
    service/
    auth/
    todo/
    security/
    ratelimit/
    cache/
    exception/
    util/


---

## 2) Authentication (JWT)

### Endpoints
- `POST /api/auth/register` (Public)
  - `POST /api/auth/login` (Public)
  - `POST /api/auth/refresh` (Refresh token)
  - `POST /api/auth/logout` (Bearer)

### Validation Rules
- Email must be valid format
  - Password length ≥ 8
  - Username length 3–30
  - Duplicate email/username => `409 Conflict`
  - Password must never be returned in responses

### Token Policy
- Access token: **15 min**
  - Refresh token: **7 days**
  - Refresh rotation on refresh
  - Logout invalidates refresh token in DB

---

## 3) Todo CRUD

### Endpoints
- `GET /api/todos` list (pagination + filters)
  - `POST /api/todos` create
  - `GET /api/todos/{id}` get single
  - `PUT /api/todos/{id}` update
  - `DELETE /api/todos/{id}` soft delete

### Filters (List)
Example:
`/api/todos?status=DONE&priority=HIGH&page=0&size=20`

---

## 4) Soft Delete (Why row exists in DB but API says "not found")

This project uses **soft delete**.

### What it means
- Delete sets `deleted_at` (timestamp)
  - Row remains in DB
  - Reads exclude deleted items

### Expected behavior
If repository reads use:
- `findByIdAndDeletedAtIsNull(todoId)`

Then:
- `deleted_at IS NULL` => visible/active
  - `deleted_at IS NOT NULL` => treated as “not found” by API

### Verify in DB
--- sql
select id, deleted_at
from todos
where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1';



5) Caching (Spring Cache + Redis)

        Caching uses cache-aside behavior and should degrade gracefully if Redis is unavailable.
        
        Cache Regions, Keys, TTL
                
        Single todo
                
        Key: todo:{userId}:{id}
                
        TTL: 10 min
                
        Invalidate on: Update / Delete
                
        Todo list
                
        Key: todos:{userId}:{queryHash}
                
        TTL: 5 min
                
        Invalidate on: Any write by user
                
        User profile
                
        Key: user:{userId}
                
        TTL: 30 min
                
        Invalidate on: Profile update
                
        Notes
                
        todoSingle is handled using @Cacheable + @CacheEvict
                
        todoList is cached manually via CacheManager (get / put)
                
        On writes, the simplest safe invalidation is clearing list cache (todoList.clear())

6) Fix: SpEL Error for Kotlin object CacheKeys (EL1004E) 

       Symptom

       EL1004E: Method call ... todo(UUID,UUID) cannot be found on type CacheKeys
    
       Cause
    
       CacheKeys is a Kotlin object. SpEL T(...) expects static members.
    
       Correct implementation (recommended)
    
       package com.example.todo.cache
       
          import java.util.UUID
    
          object CacheKeys {
    
          @JvmStatic
    
          fun todo(userId: UUID?, todoId: UUID): String =
    
          "todo:${userId ?: "anon"}:$todoId"
    
          @JvmStatic
    
          fun todos(userId: UUID?, queryHash: String): String =
    
          "todos:${userId ?: "anon"}:$queryHash"
    
          }
    
          Then SpEL works:
    
          key = "T(com.example.todo.cache.CacheKeys).todo(#userId, #todoId)"
    
          Alternative (works, less clean)
    
          key = "T(com.example.todo.cache.CacheKeys).INSTANCE.todo(#userId, #todoId)"

7) Safer Cache Key Expressions (Avoid Kotlin param-name issues)

        If #userId / #todoId cannot be resolved, use indexed params.
        
        For:
        fun getTodo(userId: UUID?, todoId: UUID): TodoResponse
        
        Use:
        key = "T(com.example.todo.cache.CacheKeys).todo(#p0, #p1)"
        
        For:
        fun updateTodo(userId: UUID?, todoId: UUID, req: TodoUpdateRequest)
        
        Use:
        key = "T(com.example.todo.cache.CacheKeys).todo(#p0, #p1)"
        
        Apply same key pattern for @CacheEvict.

8) Rate Limiting (Bucket4j + Redis)

        All endpoints (per user): 200 req / 1 min
        Auth endpoints: 10 req / 15 min
        Unauthenticated: 30 req / 1 min (by IP)
        Required headers on responses:
        X-RateLimit-Limit
        X-RateLimit-Remaining
        X-RateLimit-Reset
        On exceed:
        429 Too Many Requests
        Include Retry-After

9) Error Envelope (Consistent JSON)

       All errors follow this JSON shape:
    
       {
         "status": 404,
         "error": "Not Found",
         "message": "Todo not found",
         "path": "/api/todos/xyz",
         "timestamp": "..."
       }
    
       Common status codes:
       400 Validation failure
       401 Missing/expired token
       403 Forbidden (other user’s resource)
       404 Not found (including soft-deleted)
       409 Conflict (duplicate username/email)
       429 Too many requests

10) Common Debug Scenarios

        A) “Todo not found” but row exists
        Most likely: deleted_at is NOT NULL (soft deleted). See section 4.
        B) Cache returns stale data
        Ensure @CacheEvict key matches @Cacheable key exactly
        Prefer #p0/#p1 indexed params if eviction doesn’t trigger
        Verify Redis keys:
        todo:<userId>:<todoId>
        todos:<userId>:<queryHash>
        C) Redis down
        App should continue serving from DB; caching should fail open (no crash).

11) Example cURL (Update Todo)

        curl --location --request PUT 'http://localhost:8080/api/todos/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1' \
        --header 'Authorization: Bearer <JWT>' \
        --header 'Content-Type: application/json' \
        --header 'Accept: application/json' \
        --data '{
          "status": "IN_PROGRESS",
          "priority": "HIGH"
        }'


12) Security / Ownership

        Users can only access their own todos (else 403 Forbidden)
        Soft-deleted todos are excluded from all responses
        If you want, paste your actual cache names (`todoSingle`, `todoList`) and your actual rate-limit config class name, and I’ll align those strings exactly to your codebase.

13) DataBase and Tables, Create Scripts

    (i) For users tables

         -- Table: public.users

        -- DROP TABLE IF EXISTS public.users;
        
        CREATE TABLE IF NOT EXISTS public.users
        (
        id uuid NOT NULL DEFAULT gen_random_uuid(),
        email character varying(255) COLLATE pg_catalog."default" NOT NULL,
        username character varying(30) COLLATE pg_catalog."default" NOT NULL,
        password_hash character varying(255) COLLATE pg_catalog."default" NOT NULL,
        created_at timestamp with time zone NOT NULL DEFAULT now(),
        updated_at timestamp with time zone NOT NULL DEFAULT now(),
        CONSTRAINT users_pkey PRIMARY KEY (id)
        )
        
        TABLESPACE pg_default;
        
        ALTER TABLE IF EXISTS public.users
        OWNER to postgres;
        -- Index: ux_users_email
        
        -- DROP INDEX IF EXISTS public.ux_users_email;
        
        CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
        ON public.users USING btree
        (email COLLATE pg_catalog."default" ASC NULLS LAST)
        TABLESPACE pg_default;
        -- Index: ux_users_username
        
        -- DROP INDEX IF EXISTS public.ux_users_username;
        
        CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username
        ON public.users USING btree
        (username COLLATE pg_catalog."default" ASC NULLS LAST)
        TABLESPACE pg_default;

    (ii) For todos table
        
        -- Table: public.todos

        -- DROP TABLE IF EXISTS public.todos;
        
        CREATE TABLE IF NOT EXISTS public.todos
        (
        id uuid NOT NULL DEFAULT gen_random_uuid(),
        user_id uuid NOT NULL,
        title character varying(255) COLLATE pg_catalog."default" NOT NULL,
        description text COLLATE pg_catalog."default",
        status character varying(20) COLLATE pg_catalog."default" NOT NULL DEFAULT 'PENDING'::todo_status,
        priority character varying(10) COLLATE pg_catalog."default" NOT NULL DEFAULT 'MEDIUM'::todo_priority,
        due_date date,
        deleted_at timestamp with time zone,
        created_at timestamp with time zone NOT NULL DEFAULT now(),
        updated_at timestamp with time zone NOT NULL DEFAULT now(),
        CONSTRAINT todos_pkey PRIMARY KEY (id),
        CONSTRAINT todos_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE RESTRICT
        )
        
        TABLESPACE pg_default;
        
        ALTER TABLE IF EXISTS public.todos
        OWNER to postgres;
        -- Index: ix_todos_user_due_date
        
        -- DROP INDEX IF EXISTS public.ix_todos_user_due_date;
        
        CREATE INDEX IF NOT EXISTS ix_todos_user_due_date
        ON public.todos USING btree
        (user_id ASC NULLS LAST, due_date ASC NULLS LAST)
        TABLESPACE pg_default
        WHERE deleted_at IS NULL;
        -- Index: ix_todos_user_not_deleted
        
        -- DROP INDEX IF EXISTS public.ix_todos_user_not_deleted;
        
        CREATE INDEX IF NOT EXISTS ix_todos_user_not_deleted
        ON public.todos USING btree
        (user_id ASC NULLS LAST)
        TABLESPACE pg_default
        WHERE deleted_at IS NULL;
        -- Index: ix_todos_user_status_priority
        
        -- DROP INDEX IF EXISTS public.ix_todos_user_status_priority;
        
        CREATE INDEX IF NOT EXISTS ix_todos_user_status_priority
        ON public.todos USING btree
        (user_id ASC NULLS LAST, status COLLATE pg_catalog."default" ASC NULLS LAST, priority COLLATE pg_catalog."default" ASC NULLS LAST)
        TABLESPACE pg_default
        WHERE deleted_at IS NULL;

    (iii) For refresh_tokens table
        
        -- Table: public.refresh_tokens

        -- DROP TABLE IF EXISTS public.refresh_tokens;
        
        CREATE TABLE IF NOT EXISTS public.refresh_tokens
        (
        id uuid NOT NULL DEFAULT gen_random_uuid(),
        user_id uuid NOT NULL,
        token_hash character varying(255) COLLATE pg_catalog."default" NOT NULL,
        expires_at timestamp with time zone NOT NULL,
        revoked_at timestamp with time zone,
        replaced_by_token_id uuid,
        created_at timestamp with time zone NOT NULL DEFAULT now(),
        revoked boolean NOT NULL DEFAULT false,
        CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
        CONSTRAINT refresh_tokens_replaced_by_token_id_fkey FOREIGN KEY (replaced_by_token_id)
        REFERENCES public.refresh_tokens (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL,
        CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
        )
        
        TABLESPACE pg_default;
        
        ALTER TABLE IF EXISTS public.refresh_tokens
        OWNER to postgres;
        -- Index: ix_refresh_tokens_user_active
        
        -- DROP INDEX IF EXISTS public.ix_refresh_tokens_user_active;
        
        CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user_active
        ON public.refresh_tokens USING btree
        (user_id ASC NULLS LAST, expires_at ASC NULLS LAST, revoked_at ASC NULLS LAST)
        TABLESPACE pg_default;
        -- Index: ux_refresh_tokens_hash
        
        -- DROP INDEX IF EXISTS public.ux_refresh_tokens_hash;
        
        CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_hash
        ON public.refresh_tokens USING btree
        (token_hash COLLATE pg_catalog."default" ASC NULLS LAST)
        TABLESPACE pg_default;
