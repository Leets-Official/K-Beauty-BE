# 공유 도메인 ERD

## 테이블 관계

```mermaid
erDiagram
    RECOMMENDATIONS ||--o{ SHARES : shared_via

    SHARES {
      BIGINT id PK
      BIGINT recommendation_id FK
      varchar share_token
      datetime created_at
    }

    RECOMMENDATIONS {
      BIGINT id PK
    }
```

## 테이블 정의

### shares (공유)

| 이름 | 필드명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| 공유 ID | `id` | BIGINT | PK | 공유 식별자 |
| 추천 결과 ID | `recommendation_id` | BIGINT | NOT NULL FK → recommendations.id | 공유된 추천 결과 |
| 공유 토큰 | `share_token` | VARCHAR(36) | NOT NULL UNIQUE | 공유 링크 접근용 토큰. `/share/{share_token}` 형태로 사용. 동일 `recommendation_id`에 대해 재사용 |
| 생성일자 | `created_at` | DATETIME | NOT NULL | 공유 생성 시각 |

## 제약 조건

- 공유 기능은 `shares` 테이블로 일원화한다. `recommendations`에 공유 관련 필드를 두지 않는다.
- `share_token`은 공유 생성 시 서버에서 UUID로 발급하며, `/share/{share_token}` 형태로 접근한다.
- 동일한 `recommendation_id`에 대해 공유 요청이 중복될 경우, 기존 토큰을 재사용한다. 새 토큰을 발급하지 않는다.
