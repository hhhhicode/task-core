--liquibase formatted sql

--changeset backend:011-partial-indexes
-- 소프트 삭제된 레코드를 제외한 활성 레코드에 대한 부분 인덱스
-- 성능 최적화: deleted_at IS NULL 조건을 자주 사용하므로 인덱스로 가속화

-- todos 테이블: 활성 레코드에 대한 인덱스
CREATE INDEX IF NOT EXISTS idx_todos_active ON todos(id) WHERE deleted_at IS NULL;

-- gantt_tasks 테이블: 활성 레코드의 todo_id 조회 최적화
CREATE INDEX IF NOT EXISTS idx_gantt_tasks_active_todo ON gantt_tasks(todo_id) WHERE deleted_at IS NULL;

-- kanban_cards 테이블: 활성 레코드의 todo_id 조회 최적화
CREATE INDEX IF NOT EXISTS idx_kanban_cards_active_todo ON kanban_cards(todo_id) WHERE deleted_at IS NULL;

-- kanban_cards 테이블: column_id와 position을 함께 조회하는 경우 최적화
CREATE INDEX IF NOT EXISTS idx_kanban_cards_column_position ON kanban_cards(column_id, position) WHERE deleted_at IS NULL;

COMMENT ON INDEX idx_todos_active IS '활성 Todo 레코드 조회 최적화';
COMMENT ON INDEX idx_gantt_tasks_active_todo IS '활성 Gantt Task의 Todo 참조 최적화';
COMMENT ON INDEX idx_kanban_cards_active_todo IS '활성 Kanban Card의 Todo 참조 최적화';
COMMENT ON INDEX idx_kanban_cards_column_position IS '칸반 컬럼 내 카드 위치 조회 최적화';

--rollback DROP INDEX IF EXISTS idx_todos_active;
--rollback DROP INDEX IF EXISTS idx_gantt_tasks_active_todo;
--rollback DROP INDEX IF EXISTS idx_kanban_cards_active_todo;
--rollback DROP INDEX IF EXISTS idx_kanban_cards_column_position;
