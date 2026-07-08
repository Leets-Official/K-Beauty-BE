# Contributing Guide


## 개요
K-Beauty-B E 프로젝트의 협업 규칙을 정리한 문서입니다.

## 브랜치 전략

- `main`은 배포 가능한 안정 브랜치입니다.
- `develop`은 기능 통합 브랜치이자 기본 브랜치입니다.
- `main`, `develop`에는 직접 push하지 않습니다.
- 모든 변경은 `브랜치 -> PR -> 머지` 흐름으로 반영합니다.
- 작업은 `develop`에서 새 브랜치를 생성한 뒤 진행합니다.

## 커밋 메시지

Conventional Commits 형식을 사용합니다.

```text
<type>: <subject>
```

사용 가능한 type:

- `feat` : 신규 기능
- `fix` : 버그 수정
- `chore` : 환경/설정 변경
- `refactor` : 리팩토링
- `docs` : 문서
- `style` : 스타일 관련 수정/추가
- `test` : test 관련
- 
예시:

```text
feat/login
fix/comment
refactor/user-service
```

## PR 규칙

- PR 제목은 `[Feat] 로그인 기능 구현`처럼 대괄호 태그를 사용합니다.
- PR 본문에 연관 이슈, 작업 내용, 테스트 결과를 작성합니다.
- 유료 프롬프트 원문, 토큰, 개인정보가 로그나 응답 예시에 포함되지 않도록 주의합니다.
- 최소 1명 이상의 리뷰 승인 후 병합합니다.

허용 Type:

- `Feat`: 새로운 기능 추가
- `Fix`: 버그 수정
- `Hotfix`: 긴급 수정
- `Refactor`: 리팩토링
- `Chore`: 설정, 빌드, 기타 작업
- `Docs`: 문서 변경
- `Style`: 코드 스타일 수정
- `Test`: 테스트 추가 또는 수정

