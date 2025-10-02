--liquibase formatted sql

--changeset backend:002-create-gantt-tasks-table
CREATE TABLE IF NOT EXISTS gantt_tasks (
    id UUID PRIMARY KEY,
    todo_id UUID NOT NULL UNIQUE,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_gantt_tasks_todo FOREIGN KEY (todo_id) REFERENCES todos(id),
    CONSTRAINT chk_gantt_tasks_progress CHECK (progress >= 0 AND progress <= 100)
);

COMMENT ON TABLE gantt_tasks IS '간트 차트 상의 Todo 표현';
COMMENT ON COLUMN gantt_tasks.id IS '고유 식별자 (UUID)';
COMMENT ON COLUMN gantt_tasks.todo_id IS '연결된 Todo ID (Unique FK)';
COMMENT ON COLUMN gantt_tasks.start_date IS '시작 일시 (UTC)';
COMMENT ON COLUMN gantt_tasks.end_date IS '종료 일시 (UTC)';
COMMENT ON COLUMN gantt_tasks.progress IS '진행률 (0-100)';
COMMENT ON COLUMN gantt_tasks.deleted_at IS '소프트 삭제 시각 (NULL = 활성)';

--rollback DROP TABLE IF EXISTS gantt_tasks;
