-- Repair inconsistent local schemas where Flyway history is ahead of actual tables.
-- This keeps iterative dev environments bootable without forcing a full DB reset.

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

COMMENT ON TABLE users IS 'PaperLens 애플리케이션 사용자 계정';
COMMENT ON COLUMN users.id         IS '사용자 PK (bigserial)';
COMMENT ON COLUMN users.email      IS '로그인 이메일';
COMMENT ON COLUMN users.password   IS 'BCrypt 해시 비밀번호';
COMMENT ON COLUMN users.name       IS '사용자 이름';
COMMENT ON COLUMN users.role       IS '권한 역할';
COMMENT ON COLUMN users.created_at IS '생성 시각';
COMMENT ON COLUMN users.updated_at IS '수정 시각';
COMMENT ON COLUMN users.deleted_at IS '소프트 삭제 시각';

INSERT INTO users (email, password, name, role)
SELECT 'admin@paperlens.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin',
       'ADMIN'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin@paperlens.com'
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'documents_created_by_fkey'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT documents_created_by_fkey
            FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ai_query_logs_user_id_fkey'
    ) THEN
        ALTER TABLE ai_query_logs
            ADD CONSTRAINT ai_query_logs_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
