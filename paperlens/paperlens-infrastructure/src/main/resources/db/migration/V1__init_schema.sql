CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Users: 서비스 로그인 계정과 권한을 관리하는 기본 사용자 테이블
CREATE TABLE users (
                       id          BIGSERIAL PRIMARY KEY,
                       email       VARCHAR(255) UNIQUE NOT NULL,
                       password    VARCHAR(255) NOT NULL,
                       name        VARCHAR(100) NOT NULL,
                       role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at  TIMESTAMP
);

COMMENT ON TABLE users IS 'PaperLens 애플리케이션의 사용자 계정 (로그인/권한 관리)';
COMMENT ON COLUMN users.id         IS '내부 식별용 사용자 PK (bigserial)';
COMMENT ON COLUMN users.email      IS '로그인 및 고유 식별에 사용하는 이메일';
COMMENT ON COLUMN users.password   IS 'BCrypt 등으로 해싱된 사용자 비밀번호';
COMMENT ON COLUMN users.name       IS '사용자 표시 이름';
COMMENT ON COLUMN users.role       IS '권한 역할 (예: USER, ADMIN)';
COMMENT ON COLUMN users.created_at IS '계정 생성 시각';
COMMENT ON COLUMN users.updated_at IS '계정 정보 최종 수정 시각';
COMMENT ON COLUMN users.deleted_at IS '소프트 삭제 시각 (NULL 이면 활성 상태)';

-- Documents: 업로드된 문서의 메타데이터를 관리하는 테이블
CREATE TABLE documents (
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

COMMENT ON TABLE documents IS 'PaperLens에서 관리하는 원본 문서 메타데이터';
COMMENT ON COLUMN documents.id                 IS '문서 PK (bigserial)';
COMMENT ON COLUMN documents.title              IS '문서 제목';
COMMENT ON COLUMN documents.description        IS '문서에 대한 설명/비고';
COMMENT ON COLUMN documents.original_file_name IS '업로드 시 사용자가 가진 원본 파일명';
COMMENT ON COLUMN documents.storage_path       IS '서버/스토리지 상의 실제 저장 경로';
COMMENT ON COLUMN documents.mime_type          IS '문서 MIME 타입 (기본값: application/pdf)';
COMMENT ON COLUMN documents.page_count         IS '문서 총 페이지 수';
COMMENT ON COLUMN documents.file_size          IS '파일 크기 (byte 단위)';
COMMENT ON COLUMN documents.document_type      IS '문서 유형(카테고리) 식별용 문자열';
COMMENT ON COLUMN documents.summary_short      IS '짧은 요약문 (리스트/카드용)';
COMMENT ON COLUMN documents.summary_long       IS '긴 요약문 (상세 화면용)';
COMMENT ON COLUMN documents.status             IS '문서 처리 상태 (예: PENDING, READY, FAILED)';
COMMENT ON COLUMN documents.created_by         IS '문서를 업로드한 사용자 ID (users.id FK)';
COMMENT ON COLUMN documents.created_at         IS '문서 메타데이터 생성 시각';
COMMENT ON COLUMN documents.updated_at         IS '문서 메타데이터 최종 수정 시각';
COMMENT ON COLUMN documents.deleted_at         IS '문서 소프트 삭제 시각';

-- Document chunks: 문서를 페이지/의미 단위로 분할한 청크 텍스트와 임베딩을 저장하는 테이블
CREATE TABLE document_chunks (
                                 id           BIGSERIAL PRIMARY KEY,
                                 document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 page_from    INT NOT NULL,
                                 page_to      INT NOT NULL,
                                 chunk_index  INT NOT NULL,
                                 content      TEXT NOT NULL,
                                 content_tsv  TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
                                 embedding    VECTOR(1536),
                                 token_count  INT DEFAULT 0,
                                 created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE document_chunks IS '문서 본문을 검색/임베딩 단위로 분할한 청크 데이터';
COMMENT ON COLUMN document_chunks.id          IS '청크 PK (bigserial)';
COMMENT ON COLUMN document_chunks.document_id IS '소속 문서 ID (documents.id, ON DELETE CASCADE)';
COMMENT ON COLUMN document_chunks.page_from   IS '청크에 포함된 시작 페이지 번호';
COMMENT ON COLUMN document_chunks.page_to     IS '청크에 포함된 종료 페이지 번호';
COMMENT ON COLUMN document_chunks.chunk_index IS '문서 내 청크 인덱스 (0 or 1부터 시작하는 순번)';
COMMENT ON COLUMN document_chunks.content     IS '청크 텍스트 원문';
COMMENT ON COLUMN document_chunks.content_tsv IS 'PostgreSQL tsvector (전문 검색용 인덱스 컬럼)';
COMMENT ON COLUMN document_chunks.embedding   IS '텍스트 임베딩 벡터 (초기 1536차원, 이후 384차원으로 마이그레이션)';
COMMENT ON COLUMN document_chunks.token_count IS '청크 텍스트의 토큰 수';
COMMENT ON COLUMN document_chunks.created_at  IS '청크 생성 시각';

-- Document tags: 문서에 태그를 부여하기 위한 N:1 매핑 테이블
CREATE TABLE document_tags (
                               id          BIGSERIAL PRIMARY KEY,
                               document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                               tag_name    VARCHAR(100) NOT NULL
);

COMMENT ON TABLE document_tags IS '문서별 태그 정보를 저장하는 테이블';
COMMENT ON COLUMN document_tags.id          IS '문서 태그 매핑 PK (bigserial)';
COMMENT ON COLUMN document_tags.document_id IS '태그가 부여된 문서 ID (documents.id, ON DELETE CASCADE)';
COMMENT ON COLUMN document_tags.tag_name    IS '태그 이름 (예: 분야, 주제 등)';

-- Document jobs: 문서 처리 파이프라인의 잡 상태를 추적하는 테이블
CREATE TABLE document_jobs (
                               id            BIGSERIAL PRIMARY KEY,
                               document_id   BIGINT NOT NULL REFERENCES documents(id),
                               job_type      VARCHAR(50) NOT NULL,
                               status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               started_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                               finished_at   TIMESTAMP,
                               error_message TEXT
);

COMMENT ON TABLE document_jobs IS '문서 처리 작업(파싱, 임베딩, 요약 등)의 실행 내역 및 상태';
COMMENT ON COLUMN document_jobs.id            IS '잡 PK (bigserial)';
COMMENT ON COLUMN document_jobs.document_id   IS '대상 문서 ID (documents.id)';
COMMENT ON COLUMN document_jobs.job_type      IS '잡 유형 (예: PARSE, EMBED, SUMMARY 등)';
COMMENT ON COLUMN document_jobs.status        IS '잡 상태 (PENDING, RUNNING, SUCCESS, FAILED 등)';
COMMENT ON COLUMN document_jobs.started_at    IS '작업 시작 시각';
COMMENT ON COLUMN document_jobs.finished_at   IS '작업 종료 시각 (NULL 이면 미완료)';
COMMENT ON COLUMN document_jobs.error_message IS '실패 시 에러 메시지';

-- AI query logs: 사용자/문서 단위 AI 질의와 응답, 성능 메트릭을 저장하는 테이블
CREATE TABLE ai_query_logs (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT NOT NULL REFERENCES users(id),
                               document_id BIGINT REFERENCES documents(id),
                               question    TEXT NOT NULL,
                               answer      TEXT,
                               latency_ms  BIGINT,
                               model_name  VARCHAR(100),
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_query_logs IS 'AI 질의/응답 및 성능(레이턴시, 모델명)을 기록하는 로그 테이블';
COMMENT ON COLUMN ai_query_logs.id          IS 'AI 질의 로그 PK (bigserial)';
COMMENT ON COLUMN ai_query_logs.user_id     IS '질의를 수행한 사용자 ID (users.id)';
COMMENT ON COLUMN ai_query_logs.document_id IS '질의 대상 문서 ID (선택적, NULL 가능)';
COMMENT ON COLUMN ai_query_logs.question    IS '사용자가 입력한 질문 텍스트';
COMMENT ON COLUMN ai_query_logs.answer      IS '모델이 응답한 답변 텍스트';
COMMENT ON COLUMN ai_query_logs.latency_ms  IS '질의~응답까지 걸린 시간(ms)';
COMMENT ON COLUMN ai_query_logs.model_name  IS '해당 요청에 사용된 AI 모델 이름';
COMMENT ON COLUMN ai_query_logs.created_at  IS '로그 기록 시각';

-- Indexes
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);
CREATE INDEX idx_documents_deleted_at ON documents(deleted_at);
CREATE INDEX idx_documents_title_trgm ON documents USING gin(title gin_trgm_ops);
CREATE INDEX idx_documents_desc_trgm ON documents USING gin(coalesce(description,'') gin_trgm_ops);
CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_content_tsv ON document_chunks USING gin(content_tsv);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING hnsw(embedding vector_cosine_ops);
CREATE INDEX idx_document_tags_document_id ON document_tags(document_id);

-- admin user (password: admin123)
INSERT INTO users (email, password, name, role)
VALUES ('admin@paperlens.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'ADMIN');
