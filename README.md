# PaperLens (PdfStudy)

![PaperLens - AI 문서 검색 및 분석](imgs/이미지.jpg)


## 1. 프로젝트 소개

**PaperLens**는 PDF 문서를 업로드하고, AI 기반 검색·요약·질의응답을 제공하는 문서 관리 플랫폼입니다.

- **목적**: PDF를 저장하고 내용을 검색·분석하여 필요한 정보를 빠르게 찾을 수 있도록 지원
- **기술 포인트**: 벡터 임베딩(pgvector)과 키워드 검색을 결합한 **하이브리드 검색**, Spring AI를 활용한 **문서 요약·QA**
- **대상**: 내부 문서(계약서, 매뉴얼, 제안서, 보고서 등)를 체계적으로 관리하고 싶은 팀·개인

---

## 2. 프로젝트 기능

<img width="1206" height="653" alt="image" src="https://github.com/user-attachments/assets/befa27cc-e2ee-45b8-85c5-955f7b50b8d2" />

### 인증

- 이메일/비밀번호 로그인
- JWT 기반 인증
- 역할: 일반 사용자(USER), 관리자(ADMIN)

### 문서 관리

- **PDF 업로드**: 파일 업로드 후 자동 메타정보 추출 및 인덱싱
- **문서 목록**: 페이지네이션, 상태(대기/처리중/완료/실패) 표시
- **문서 상세**: PDF 뷰어, 메타정보·요약·태그·다운로드
- **문서 삭제**: 소프트 삭제(deleted_at)

### 검색

- **하이브리드**: 키워드 + 의미(벡터) 검색 결합
- **키워드**: 전통적인 전문 검색
- **의미 검색**: 임베딩 유사도 기반
- **푸지**: 오타 허용 검색
- 문서 유형 필터(계약서, 매뉴얼, 제안서 등)

### AI 기능

- **문서 요약**: 3줄 요약(summary_short), 상세 요약(summary_long)
- **문서 기반 Q&A**: 선택한 문서에 대해 질문하면 답변 + 출처(챕터/페이지) 제공
- **유사 문서**: 현재 문서와 의미적으로 유사한 문서 추천

### 관리자

- **대시보드**: 전체/인덱싱완료/실패 문서 수, 인덱싱 성공률, 총 질문 수, 평균 응답 시간
- **실패 문서 재처리**: 상태가 FAILED인 문서 재인덱싱

---

## 3. ERD

```mermaid
erDiagram
    users ||--o{ documents : "created_by"
    users ||--o{ ai_query_logs : "user_id"
    documents ||--o{ document_chunks : "1:N"
    documents ||--o{ document_tags : "1:N"
    documents ||--o{ document_jobs : "document_id"
    documents ||--o{ ai_query_logs : "document_id"

    users {
        bigint id PK
        varchar email UK
        varchar password
        varchar name
        varchar role
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    documents {
        bigint id PK
        varchar title
        text description
        varchar original_file_name
        varchar storage_path
        varchar mime_type
        int page_count
        bigint file_size
        varchar document_type
        text summary_short
        text summary_long
        varchar status
        bigint created_by FK
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at
    }

    document_chunks {
        bigint id PK
        bigint document_id FK
        int page_from
        int page_to
        int chunk_index
        text content
        vector embedding
        int token_count
        timestamp created_at
    }

    document_tags {
        bigint id PK
        bigint document_id FK
        varchar tag_name
    }

    document_jobs {
        bigint id PK
        bigint document_id FK
        varchar job_type
        varchar status
        timestamp started_at
        timestamp finished_at
        text error_message
    }

    ai_query_logs {
        bigint id PK
        bigint user_id FK
        bigint document_id FK
        text question
        text answer
        bigint latency_ms
        varchar model_name
        timestamp created_at
    }

    ai_model_configs {
        bigint id PK
        varchar model_name
        varchar model_type
        varchar endpoint
        boolean is_active
        jsonb config_json
        timestamp created_at
        timestamp updated_at
    }
```

### 테이블 요약

| 테이블             | 설명                                  |
| ------------------ | ------------------------------------- |
| `users`            | 사용자(이메일, 역할 등)               |
| `documents`        | PDF 메타정보, 요약, 상태              |
| `document_chunks`  | 문서를 나눈 텍스트 조각 + 벡터 임베딩 |
| `document_tags`    | 문서별 태그                           |
| `document_jobs`    | 인덱싱/처리 작업 큐                   |
| `ai_query_logs`    | Q&A 질문·답변·응답시간 로그           |
| `ai_model_configs` | 임베딩/챗 모델 설정                   |

---

## 4. 프로젝트 아키텍처

### 전체 구성도

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client (Browser)                               │
│  Vue 3 + Vue Router + Pinia + Tailwind CSS + PDF.js + Chart.js           │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ HTTP /api/* (proxy → :8080)
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Backend (Spring Boot 3 + Kotlin)                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
│  │ Auth        │ │ Document    │ │ Search      │ │ AI (Summary, QA,    │ │
│  │ (JWT)       │ │ (CRUD, Job) │ │ (Hybrid)    │ │  Embedding, Similar)│ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────────┘ │
│  ┌─────────────┐ ┌─────────────┐                                         │
│  │ Viewer      │ │ Admin       │  Spring Security, JPA, Spring AI       │
│  │ (PDF serve) │ │ (Stats)      │                                         │
│  └─────────────┘ └─────────────┘                                         │
└──────────┬──────────────────────────────┬───────────────────┬──────────┘
           │                              │                   │
           ▼                              ▼                   ▼
┌──────────────────────┐    ┌──────────────────────┐   ┌─────────────┐
│  PostgreSQL 16       │    │  Redis 7              │   │  OpenAI API │
│  + pgvector          │    │  (캐시/세션 등)        │   │  (선택)     │
│  + Flyway migration  │    │                      │   │  Embedding  │
└──────────────────────┘    └──────────────────────┘   │  Chat       │
                                                        └─────────────┘
```

### 스택 요약

| 구분         | 기술                                                                                                            |
| ------------ | --------------------------------------------------------------------------------------------------------------- |
| **Frontend** | Vue 3, TypeScript, Vite, Vue Router, Pinia, Tailwind CSS, Axios, PDF.js, Chart.js, Lucide Icons                 |
| **Backend**  | Spring Boot 3.3, Kotlin 2.0, Spring Security, JPA, Spring AI (OpenAI / Transformers), JWT, Flyway, PDFBox, Tika |
| **DB**       | PostgreSQL 16 + pgvector (벡터 검색), Redis 7                                                                   |
| **인프라**   | Docker Compose (PostgreSQL, Redis)                                                                              |

---

## 5. 프로젝트 디렉터리 구조

이 저장소(`PdfStudy`)는 PaperLens의 설계/실험용 공간으로, 실제 애플리케이션과 UI 프로토타입을 함께 관리합니다.

```text
PdfStudy/
├─ README.md                  # 현재 문서
├─ frontend/                  # 최신 Vue 3 + Vite 기반 실제 SPA 프론트엔드
│  ├─ index.html
│  ├─ vite.config.ts
│  ├─ tsconfig*.json
│  ├─ src/
│  │  ├─ main.ts              # 앱 엔트리
│  │  ├─ App.vue
│  │  ├─ router/              # 라우팅 (로그인, 문서 목록/상세, 관리자 등)
│  │  ├─ stores/              # Pinia 스토어 (auth, document 등)
│  │  ├─ layouts/             # `AppLayout` 등 공통 레이아웃
│  │  ├─ views/               # 페이지 단위 뷰
│  │  └─ components/          # 공통/도메인 컴포넌트
│  └─ package.json            # `frontend` 앱 의존성 정의
│
├─ docs/
│  ├─ frontend/               # PaperLens UI/플로우를 문서화하는 샘플/데모 앱
│  │  ├─ index.html
│  │  ├─ vite.config.ts
│  │  ├─ src/
│  │  │  ├─ layouts/AppLayout.vue
│  │  │  ├─ components/
│  │  │  │  ├─ document/      # `DocumentCard`, `UploadModal` 등 문서 카드/업로드 UI
│  │  │  │  ├─ viewer/        # `PdfViewer` 등 뷰어 관련 컴포넌트
│  │  │  │  ├─ ai/            # `QaPanel`, `SimilarDocuments` 등 AI 패널
│  │  │  │  └─ common/        # `StatusBadge` 등 공통 컴포넌트
│  │  └─ package.json         # docs 용 프론트엔드 의존성
│  └─ commit.md               # 프론트엔드 관련 커밋 요약 템플릿
│
└─ imgs/                      # README에서 사용하는 이미지 자산
```

간단히 요약하면:

- **`frontend/`**: 실제 서비스용 SPA 프론트엔드 코드
- **`docs/frontend/`**: 문서/프로토타입용 프론트엔드 (UX 플로우를 설명하는 데 사용)

---

## 6. 백엔드 패키지 구조(요약)

PaperLens 백엔드는 별도 모듈/저장소에서 관리되지만, 전반적인 패키지 구조는 다음과 같이 구성됩니다.

```mermaid
flowchart TD
    A[com.sleekydz86.paperlens] --> B[domain]
    A --> C[application]
    A --> D[infrastructure]

    D --> D1[web\n(AuthController,\nDocumentController,\nSearchController,\nAiController,\nAdminController,\nViewerController)]
    D --> D2[persistence\n(entity, repository,\nmapper, adapter)]
    D --> D3[global.config\n(SecurityConfig,\nApplicationConfig,\nCustomUserDetailsService)]
    D --> D4[global.adapter\n(PdfAdapter,\nEmbeddingAdapter,\nVectorSearchAdapter,\nFileStorageAdapter,\nTokenAdapter,\nAiAdapter,\nAuthAdapter,\nDocumentProcessAdapter,\nQueryLogAdapter)]
    D --> D5[global.search\n(KeywordSearch,\nSemanticSearch,\nHybridSearch,\nFuzzySearch)]
```

- **`web`**: REST 컨트롤러 계층 (인증, 문서, 검색, AI, 관리자, 뷰어 엔드포인트)
- **`persistence`**: JPA 엔티티, 리포지토리, 매퍼, 도메인 리포지토리 어댑터
- **`global.config`**: Spring Security, 애플리케이션 전역 설정
- **`global.adapter`**: PDF 파싱, 임베딩/벡터 검색, 파일 스토리지, 토큰 계산, AI/인증/배치 처리 어댑터
- **`global.search`**: 키워드/의미/하이브리드/퍼지 검색 전략

위 구조를 기준으로 프론트엔드(`frontend/`)는 `/api/*` 엔드포인트를 통해 백엔드의 `web` 계층과 통신하며, 검색/AI/문서 관리 흐름을 하나의 워크스페이스 UI로 묶어 제공합니다.
