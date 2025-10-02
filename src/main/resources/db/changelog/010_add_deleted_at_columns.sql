--liquibase formatted sql

--changeset backend:010-add-deleted-at
-- Note: deleted_at 컬럼은 이미 테이블 생성 시 포함되어 있습니다.
-- 이 마이그레이션은 기존 테이블에 deleted_at을 추가하는 경우를 위한 것입니다.
-- 현재는 이미 포함되어 있으므로 실행할 내용이 없습니다.

-- 향후 기존 테이블에 소프트 삭제를 추가할 경우 아래 패턴을 사용:
-- ALTER TABLE todos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
-- ALTER TABLE gantt_tasks ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
-- ALTER TABLE kanban_cards ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

--rollback -- No rollback needed
