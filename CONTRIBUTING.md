# Contributing Guide

K-Beauty-BE 프로젝트의 협업 규칙을 정리한 문서입니다.

## 브랜치 전략

### 브랜치 기본 규칙

- `main`은 배포 가능한 안정 브랜치입니다.
- `develop`은 기능 통합 브랜치이자 기본 브랜치입니다.
- `main`, `develop`에는 직접 push하지 않습니다.
- 모든 변경은 `브랜치 -> PR -> 머지` 흐름으로 반영합니다.
- 작업은 `develop`에서 새 브랜치를 생성한 뒤 진행합니다.

### 브랜치 이름

브랜치 이름은 아래 형식을 따릅니다.

```text
<type>/<작업-내용>
```

예시:

```text
feat/login
fix/comment
refactor/user-service
```

주요 type:

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 변경
- `test`: 테스트 추가 또는 수정
- `chore`: 설정, 빌드, 기타 작업

## 이슈 및 PR 제목 규칙

이슈와 PR 제목은 아래 형식을 따릅니다.

```text
[<type>/<작업-내용>] <짧은 설명>
```

예시:

```text
[feat/login] AuthService 구현
[fix/comment] comment 테이블 수정
```

제목의 type과 작업 내용은 가능하면 브랜치 이름과 동일하게 맞춥니다.

## 커밋 및 PR 규칙

- PR 제목은 이슈 및 PR 제목 규칙을 따릅니다.
- PR 본문에는 관련 이슈를 연결합니다. 예: `Closes #4`
- PR에는 작업 내용, 변경 유형, 테스트 여부를 작성합니다.
- PR은 최소 1명 이상 승인 후 merge합니다.
- 기능 추가나 버그 수정은 가능한 작은 단위로 나누어 진행합니다.
