--liquibase formatted sql

--changeset backend:003-create-kanban-cards-table
CREATE TABLE IF NOT EXISTS kanban_cards (
    id UUID PRIMARY KEY,
    todo_id UUID NOT NULL UNIQUE,
    column_id VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_kanban_cards_todo FOREIGN KEY (todo_id) REFERENCES todos(id),
    CONSTRAINT chk_kanban_cards_position CHECK (position >= 0)
);

COMMENT ON TABLE kanban_cards IS '칸반 보드 상의 Todo 표현';
COMMENT ON COLUMN kanban_cards.id IS '고유 식별자 (UUID)';
COMMENT ON COLUMN kanban_cards.todo_id IS '연결된 Todo ID (Unique FK)';
COMMENT ON COLUMN kanban_cards.column_id IS '칸반 컬럼 ID (최대 255자)';
COMMENT ON COLUMN kanban_cards.position IS '컬럼 내 위치 (0 이상)';
COMMENT ON COLUMN kanban_cards.deleted_at IS '소프트 삭제 시각 (NULL = 활성)';

--rollback DROP TABLE IF EXISTS kanban_cards;
