-- AI 모델 설정 테이블: 임베딩/챗 모델 메타데이터와 엔드포인트, 동적 설정을 관리
CREATE TABLE ai_model_configs (
                                  id          BIGSERIAL PRIMARY KEY,
                                  model_name  VARCHAR(100) NOT NULL,
                                  model_type  VARCHAR(50) NOT NULL, -- 예: EMBEDDING, CHAT
                                  endpoint    VARCHAR(500),
                                  is_active   BOOLEAN DEFAULT TRUE,
                                  config_json JSONB,
                                  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_model_configs IS '임베딩/챗 등 AI 모델 구성을 관리하는 설정 테이블';
COMMENT ON COLUMN ai_model_configs.id          IS 'AI 모델 설정 PK (bigserial)';
COMMENT ON COLUMN ai_model_configs.model_name  IS '모델 이름 또는 별칭 (예: all-MiniLM-L6-v2)';
COMMENT ON COLUMN ai_model_configs.model_type  IS '모델 타입 (EMBEDDING, CHAT 등)';
COMMENT ON COLUMN ai_model_configs.endpoint    IS '모델이 배포된 HTTP 엔드포인트 URL (옵션)';
COMMENT ON COLUMN ai_model_configs.is_active   IS '활성 여부 플래그 (운영 중 사용 여부 제어)';
COMMENT ON COLUMN ai_model_configs.config_json IS '모델 별 전용 설정(JSON) (예: 파라미터, 토큰 제한 등)';
COMMENT ON COLUMN ai_model_configs.created_at  IS '구성 생성 시각';
COMMENT ON COLUMN ai_model_configs.updated_at  IS '구성 최종 수정 시각';

-- 초기 예시 모델 설정: 실제 운영 환경에서는 벤더/모델명에 맞게 교체
INSERT INTO ai_model_configs (model_name, model_type, is_active)
VALUES
    ('text-embedding-3-small', 'EMBEDDING', TRUE),
    ('gpt-4o-mini', 'CHAT', TRUE);
