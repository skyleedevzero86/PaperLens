-- 기존 벡터 데이터는 삭제되며, 문서 재처리 시 최신 모델 기준으로 재생성됨.

-- 기존 임베딩 인덱스 제거
DROP INDEX IF EXISTS idx_document_chunks_embedding;

-- 기존 1536차원 임베딩 컬럼 제거 후 384차원 벡터 컬럼으로 재정의
ALTER TABLE document_chunks DROP COLUMN IF EXISTS embedding;
ALTER TABLE document_chunks ADD COLUMN embedding vector(384);

COMMENT ON COLUMN document_chunks.embedding IS '텍스트 임베딩 벡터 (Hugging Face all-MiniLM 계열, 384차원)';

-- 384차원 임베딩 벡터에 대해 HNSW + cosine distance 인덱스를 재생성
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING hnsw(embedding vector_cosine_ops);
