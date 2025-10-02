--liquibase formatted sql

--changeset backend:001-create-todos-table
CREATE TABLE IF NOT EXISTS todos (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE todos IS 'Todo 작업의 원자 단위';
COMMENT ON COLUMN todos.id IS '고유 식별자 (UUID)';
COMMENT ON COLUMN todos.title IS '제목 (최대 255자, 빈 문자열 금지)';
COMMENT ON COLUMN todos.description IS '설명 (선택사항)';
COMMENT ON COLUMN todos.status IS '상태 (TODO, IN_PROGRESS, DONE) - 코드 관리';
COMMENT ON COLUMN todos.priority IS '우선순위 (LOW, MEDIUM, HIGH) - 코드 관리';
COMMENT ON COLUMN todos.created_at IS '생성 시각 (UTC)';
COMMENT ON COLUMN todos.updated_at IS '수정 시각 (UTC)';
COMMENT ON COLUMN todos.deleted_at IS '소프트 삭제 시각 (NULL = 활성)';

--rollback DROP TABLE IF EXISTS todos;
