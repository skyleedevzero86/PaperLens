-- Repair local/dev databases that were baselined before the initial schema actually ran.
-- This keeps fresh pgvector containers bootable without manual DB resets.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS documents (
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(500) NOT NULL,
    description        TEXT,
    original_file_name VARCHAR(500) NOT NULL,
    storage_path       VARCHAR(1000) NOT NULL,
    mime_type          VARCHAR(100) DEFAULT 'application/pdf',
    page_count         INT DEFAULT 0,
    file_size          BIGINT DEFAULT 0,
    document_type      VARCHAR(100),
    summary_short      TEXT,
    summary_long       TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by         BIGINT NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

ALTER TABLE documents ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100) DEFAULT 'application/pdf';
ALTER TABLE documents ADD COLUMN IF NOT EXISTS page_count INT DEFAULT 0;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS file_size BIGINT DEFAULT 0;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS document_type VARCHAR(100);
ALTER TABLE documents ADD COLUMN IF NOT EXISTS summary_short TEXT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS summary_long TEXT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE documents ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE documents ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS document_chunks (
    id           BIGSERIAL PRIMARY KEY,
    document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_from    INT NOT NULL,
    page_to      INT NOT NULL,
    chunk_index  INT NOT NULL,
    content      TEXT NOT NULL,
    content_tsv  TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    embedding    VECTOR(384),
    token_count  INT DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS page_from INT NOT NULL DEFAULT 0;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS page_to INT NOT NULL DEFAULT 0;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS chunk_index INT NOT NULL DEFAULT 0;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS content TEXT NOT NULL DEFAULT '';
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS token_count INT DEFAULT 0;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS content_tsv TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

DROP INDEX IF EXISTS idx_document_chunks_embedding;
ALTER TABLE document_chunks DROP COLUMN IF EXISTS embedding;
ALTER TABLE document_chunks ADD COLUMN embedding VECTOR(384);

CREATE TABLE IF NOT EXISTS document_tags (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_name    VARCHAR(100) NOT NULL
);

ALTER TABLE document_tags ADD COLUMN IF NOT EXISTS tag_name VARCHAR(100) NOT NULL DEFAULT '';

CREATE TABLE IF NOT EXISTS document_jobs (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT NOT NULL REFERENCES documents(id),
    job_type      VARCHAR(50) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at   TIMESTAMP,
    error_message TEXT
);

ALTER TABLE document_jobs ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE document_jobs ADD COLUMN IF NOT EXISTS started_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE document_jobs ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;
ALTER TABLE document_jobs ADD COLUMN IF NOT EXISTS error_message TEXT;

CREATE TABLE IF NOT EXISTS ai_query_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    document_id BIGINT REFERENCES documents(id),
    question    TEXT NOT NULL,
    answer      TEXT,
    latency_ms  BIGINT,
    model_name  VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_model_configs (
    id          BIGSERIAL PRIMARY KEY,
    model_name  VARCHAR(100) NOT NULL,
    model_type  VARCHAR(50) NOT NULL,
    endpoint    VARCHAR(500),
    is_active   BOOLEAN DEFAULT TRUE,
    config_json JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_deleted_at ON documents(deleted_at);
CREATE INDEX IF NOT EXISTS idx_documents_title_trgm ON documents USING gin(title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_documents_desc_trgm ON documents USING gin(coalesce(description,'') gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_tsv ON document_chunks USING gin(content_tsv);
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING hnsw(embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_document_tags_document_id ON document_tags(document_id);

INSERT INTO ai_model_configs (model_name, model_type, is_active)
SELECT 'text-embedding-3-small', 'EMBEDDING', TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_model_configs
    WHERE model_name = 'text-embedding-3-small'
      AND model_type = 'EMBEDDING'
);

INSERT INTO ai_model_configs (model_name, model_type, is_active)
SELECT 'huggingface', 'CHAT', TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_model_configs
    WHERE model_name = 'huggingface'
      AND model_type = 'CHAT'
);

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
