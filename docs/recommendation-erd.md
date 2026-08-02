# 추천 도메인 ERD

## 테이블 관계

```mermaid
erDiagram
    RECOMMENDATIONS ||--o{ RECOMMENDATION_STEPS : contains
    SESSIONS ||--o{ RECOMMENDATIONS : triggers
    USER_CONDITIONS ||--o{ RECOMMENDATIONS : based_on
    PRODUCTS ||--o{ RECOMMENDATION_CANDIDATES : included_in
    RECOMMENDATION_STEPS ||--o{ RECOMMENDATION_CANDIDATES : has

    RECOMMENDATIONS {
      BIGINT id PK
      BIGINT session_id FK
      BIGINT user_condition_id FK
      varchar status
      datetime created_at
      datetime updated_at
    }

    RECOMMENDATION_STEPS {
      BIGINT id PK
      BIGINT recommendation_id FK
      int step
      varchar role
      BIGINT selected_candidate_id FK
    }

    RECOMMENDATION_CANDIDATES {
      BIGINT id PK
      BIGINT step_id FK
      BIGINT product_id FK
      int candidate_rank
      boolean is_user_selected
      decimal match_score
      varchar reason_short
      datetime created_at
    }

    USER_CONDITIONS {
      BIGINT id PK
    }

    SESSIONS {
      BIGINT id PK
    }

    PRODUCTS {
      BIGINT id PK
    }
```

## 테이블 정의

### recommendations (추천 결과)

| 이름 | 필드명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| 추천 결과 ID | `id` | BIGINT | PK | 추천 결과 식별자 |
| 세션 ID | `session_id` | BIGINT | NOT NULL FK → sessions.id | 추천이 발생한 세션 |
| 조건 ID | `user_condition_id` | BIGINT | NOT NULL FK → user_conditions.id | 추천의 근거가 된 진단 조건값 |
| 상태 | `status` | VARCHAR(20) | NOT NULL | GENERATED(유효) / INVALIDATED(무효화) |
| 생성일자 | `created_at` | DATETIME | NOT NULL | 추천 생성 시각 |
| 수정일자 | `updated_at` | DATETIME | NOT NULL | 추천 마지막 갱신 시각 |

### recommendation_steps (추천 단계)

| 이름 | 필드명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| 단계 ID | `id` | BIGINT | PK | 단계 식별자 |
| 추천 결과 ID | `recommendation_id` | BIGINT | NOT NULL FK → recommendations.id | 연결된 추천 결과 |
| 루틴 단계 | `step` | INT | NOT NULL | 1(피부결 정돈) / 2(집중 케어) / 3(보습 마무리) |
| 단계 역할 | `role` | VARCHAR(20) | NOT NULL | TEXTURE(피부결 정돈) / INTENSIVE(집중 케어) / MOISTURE(보습 마무리) |
| 선택된 후보 ID | `selected_candidate_id` | BIGINT | NULL FK → recommendation_candidates.id | 현재 선택된 후보. 기본값은 candidate_rank=1, 사용자 교체 시 변경 |

### recommendation_candidates (추천 후보)

| 이름 | 필드명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| 추천 후보 ID | `id` | BIGINT | PK | 추천 후보 식별자 |
| 단계 ID | `step_id` | BIGINT | NOT NULL FK → recommendation_steps.id | 연결된 추천 단계 |
| 상품 ID | `product_id` | BIGINT | NOT NULL FK → products.id | 추천된 상품 |
| 후보 순서 | `candidate_rank` | INT | NOT NULL | 1=메인 추천, 2·3=다른 후보 |
| 사용자 교체 여부 | `is_user_selected` | BOOLEAN | NOT NULL DEFAULT FALSE | 사용자가 직접 후보로 교체한 경우 TRUE |
| 매칭 점수 | `match_score` | DECIMAL(5,2) | NULL | 상품 매칭 점수 |
| 추천 이유 요약 | `reason_short` | VARCHAR(200) | NULL | 추천 이유 한 줄 설명 |
| 생성일자 | `created_at` | DATETIME | NOT NULL | 생성 시각 |

## 제약 조건

- 같은 추천 결과 안에서 동일한 단계·후보 순서는 중복될 수 없다. `UNIQUE(step_id, candidate_rank)`
- 각 단계의 메인 추천은 `candidate_rank = 1`이며, 후보는 `candidate_rank = 2 또는 3`이다.
- `candidate_rank`는 `match_score` 내림차순으로 결정하며, 동점일 경우 `product_id` 내림차순(더 최근 등록 상품 우선)으로 결정한다.
- `is_user_selected = TRUE`인 상품은 사용자가 후보에서 직접 교체한 상품이다. 답변 변경 시 FALSE로 초기화한다.
