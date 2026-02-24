-- =========================
-- Create Enum Types
-- =========================

CREATE TYPE todo_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED'
);

CREATE TYPE todo_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH'
);

      --(i) For users tables

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

    --  (ii) For todos table
        
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

    -- (iii) For refresh_tokens table
        
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