# K-Beauty 도메인 용어 v2

> 역할: 로직·ERD·API가 공통으로 쓰는 용어의 단일 출처다.

[개요](README.md) · [도메인 용어](DOMAIN_TERMS.md) · [도메인 로직](LOGIC.md) · [ERD](ERD.md)

| 용어 | 테이블 | 정의 |
|---|---|---|
| UUID v7 | 공통 | 모든 PK·FK에 사용하는 시간 순서형 식별자. 애플리케이션에서 생성하고 MySQL에는 `BINARY(16)`으로 저장한다. |
| USER | `users` | 회원가입 전 방문 기록을 묶는 익명 주체. 실제 사람 자체를 뜻하지 않는다. |
| MEMBER | 외부 도메인 | 회원가입을 완료한 사용자. 이 문서는 MEMBER 모델을 만들지 않고 USER와 연결하는 계약만 정의한다. |
| 익명 ID | `users.anonymous_id_hash` | 브라우저·앱 설치에 발급하는 난수의 해시. 같은 값이면 같은 USER로 재방문을 연결한다. |
| 방문 세션 | `sessions` | USER의 한 번의 연속 방문. 이벤트·설문·추천의 공통 맥락이다. |
| 행동 이벤트 | `session_events` | 세션 안에서 발생한 원천 행동 기록. 설문 답변이나 추천 결과의 정본은 아니다. |
| 식별 확정 | `user_identity_resolutions` | 인증된 MEMBER 사건 또는 검증된 운영 정정으로 두 USER의 연결을 확정한 기록. |
| 제품 | `products` | 추천하거나 표시할 화장품의 기준 데이터. |
| 성분 | `ingredients` | 제품 전성분에 쓰는 정규 성분명. |
| 제품 성분 | `product_ingredients` | 제품과 성분의 포함 관계 및 표시 순서. |
| 설문 정의 | `survey_flows` | 버전으로 관리하는 질문·선택지 묶음. |
| 설문 시도 | `survey_attempts` | 한 세션에서 설문을 진행한 한 번의 과정. |
| 추천 실행 | `recommendation_runs` | 완료 설문을 입력으로 만든 한 번의 추천 결과. |
| 추천 항목 | `recommendation_items` | 추천 실행에 포함된 제품과 순위·근거 스냅샷. |
