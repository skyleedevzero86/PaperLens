# PaperLens

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

| 테이블             | 설명                                   |
| ------------------ | -------------------------------------- |
| `users`            | 사용자(이메일, 역할 등)                  |
| `documents`        | PDF 메타정보, 요약, 상태                |
| `document_chunks`  | 문서를 나눈 텍스트 조각 + 벡터 임베딩    |
| `document_tags`    | 문서별 태그                            |
| `document_jobs`    | 인덱싱/처리 작업 큐                     |
| `ai_query_logs`    | Q&A 질문·답변·응답시간 로그             |
| `ai_model_configs` | 임베딩/챗 모델 설정                     |

---

## 4. 프로젝트 아키텍처

### 전체 구성도

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client (Browser)                              │
│  Vue 3 + Vue Router + Pinia + Tailwind CSS + PDF.js + Chart.js          │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ HTTP /api/* (Vite proxy → backend :8080)
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     Backend (Spring Boot 3 + Kotlin)                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
│  │ Auth        │ │ Document    │ │ Search      │ │ AI (Summary, QA,    │ │
│  │ (JWT)       │ │ (CRUD, Job) │ │ (Hybrid)    │ │  Embedding, Similar)│ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────────┘ │
│  ┌─────────────┐ ┌─────────────┐                                         │
│  │ Viewer      │ │ Admin       │  Spring Security, JPA, Spring AI        │
│  │ (PDF serve) │ │ (Stats)      │                                        │
│  └─────────────┘ └─────────────┘                                         │
└──────────┬──────────────────────────────┬───────────────────┬────────────┘
           │                              │                   │
           ▼                              ▼                   ▼
┌──────────────────────┐    ┌──────────────────────┐   ┌─────────────────────┐
│  PostgreSQL 16       │    │  Redis 7             │   │  AI (설정에 따라)    │
│  + pgvector          │    │  (캐시/세션 등)       │   │  Hugging Face /     │
│  + Flyway migration  │    │                      │   │  Transformers(ONNX) │
└──────────────────────┘    └──────────────────────┘   └─────────────────────┘
```

### 스택 요약

| 구분         | 기술                                                                                                                   |
| ------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **Frontend** | Vue 3, TypeScript, Vite 6, Vue Router, Pinia, Tailwind CSS, Axios, PDF.js (pdfjs-dist), Chart.js, VueUse, Lucide Icons |
| **Backend**  | Spring Boot 3.5, Kotlin 2.2, Spring Security, JPA, Spring AI (Hugging Face / Transformers), JWT, Flyway, PDFBox, Tika  |
| **DB**       | PostgreSQL 16 + pgvector (벡터 검색), Redis 7                                                                          |
| **인프라**   | Docker Compose (PostgreSQL, Redis) — `docs/docker-compose.yml`                                                         |

---

## 5. 프로젝트 디렉터리 구조

이 저장소(`PaperLens`)는 PaperLens의 프론트엔드·백엔드·인프라를 한 워크스페이스에서 관리합니다.

```text
PdfStudy/
├─ README.md                  # 현재 문서
├─ imgs/                      # README 이미지 자산
│
├─ frontend/                  # Vue 3 + Vite 기반 SPA (실제 서비스용)
│  ├─ index.html
│  ├─ vite.config.ts          # dev 포트 5173, /api → localhost:8080 프록시
│  ├─ tsconfig*.json
│  ├─ package.json            # pnpm, Vue 3, Vue Router, Pinia, Tailwind, Axios, pdfjs-dist, Chart.js 등
│  └─ src/
│     ├─ main.ts              # 앱 엔트리
│     ├─ App.vue
│     ├─ router/              # 라우팅 (로그인, 문서 목록/상세, 관리자)
│     ├─ stores/              # Pinia (auth, document)
│     ├─ layouts/             # AppLayout
│     ├─ views/               # LoginView, DocumentListView, DocumentDetailView, AdminView
│     ├─ components/          # document/, viewer/, ai/, admin/, common/
│     ├─ lib/                 # api.ts (Axios 인스턴스, JWT 인터셉터)
│     └─ types/               # 공통 타입
│
├─ paperlens/                 # 백엔드 (Gradle 멀티모듈, Kotlin + Spring Boot)
   ├─ build.gradle.kts
   ├─ settings.gradle.kts     # paperlens-domain, paperlens-application, paperlens-infrastructure
   ├─ paperlens-domain/       # 도메인 엔티티·유스케이스
   ├─ paperlens-application/  # 애플리케이션 서비스 계층
   └─ paperlens-infrastructure/   # 실행 가능한 Spring Boot 앱 (REST, JPA, Security, Spring AI)
      ├─ build.gradle.kts     # bootJar → paperlens-backend.jar
      └─ src/main/resources/
         └─ application.yml  # DB, JWT, Flyway, Spring AI( Hugging Face / Transformers ), pgvector
```

요약:

- **`frontend/`**: 서비스용 SPA. `/api` 요청은 Vite 개발 서버가 백엔드(8080)로 프록시.
- **`paperlens/`**: 백엔드. 실행 진입점은 `paperlens-infrastructure` (Spring Boot).
- **`docs/docker-compose.yml`**: DB·Redis 로컬 실행용.

---

## 6. 백엔드 패키지 구조(요약)

PaperLens 백엔드는 이 저장소의 `paperlens/` Gradle 멀티모듈로 구성되며, 전반적인 패키지 구조는 다음과 같습니다.

<img width="1027" height="401" alt="image" src="https://github.com/user-attachments/assets/a01021fa-1614-4edb-924d-006b9d6a1b2c" />

- **`web`**: REST 컨트롤러 계층 (인증, 문서, 검색, AI, 관리자, 뷰어 엔드포인트)
- **`persistence`**: JPA 엔티티, 리포지토리, 매퍼, 도메인 리포지토리 어댑터
- **`global.config`**: Spring Security, 애플리케이션 전역 설정
- **`global.adapter`**: PDF 파싱, 임베딩/벡터 검색, 파일 스토리지, 토큰 계산, AI/인증/배치 처리 어댑터
- **`global.search`**: 키워드/의미/하이브리드/퍼지 검색 전략

프론트엔드(`frontend/`)는 `/api/*`로 백엔드(`paperlens-infrastructure`의 `web` 계층)와 통신하며, 검색·AI·문서 관리를 하나의 워크스페이스 UI로 제공합니다.
