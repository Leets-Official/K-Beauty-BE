# K-Beauty-BE

K-Beauty 서비스의 백엔드 API 서버입니다.

## 기술 스택

- Spring Boot 3.5.16

## 개발 환경

- Java 17
- Gradle Wrapper 8.14.5
- Docker
- MySQL 8.4

## 로컬 실행

로컬 개발 환경은 Docker 기반 MySQL을 사용합니다.

### 1. 환경변수 파일 생성

`.env.example` 파일을 복사해 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

`.env` 파일에는 로컬 DB 계정, 비밀번호, DB URL이 포함되므로 Git에 커밋하지 않습니다.

### 2. MySQL 실행

```bash
docker compose up -d
```

### 3. 테스트 실행

`local` profile로 MySQL 연결을 확인합니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew test
```

### 4. 애플리케이션 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

자세한 로컬 DB 연결 방법은 Notion의 로컬 MySQL DB 연결 문서를 참고합니다.

## 협업 가이드

브랜치 전략, PR 제목 규칙, PR 머지 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고합니다.
