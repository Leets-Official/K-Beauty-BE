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

> **자동화 테스트를 실행할 때는 위 명령을 사용하지 않습니다.** 아래 `자동화 테스트 실행`을 따르세요.
> `local` profile을 지정하더라도 Testcontainers 설정이 우선 적용되어, 개발용 DB가 아닌 테스트 전용 컨테이너에 연결됩니다.

#### 자동화 테스트 실행

자동화 테스트는 Testcontainers가 MySQL 8.4 컨테이너를 자동으로 띄워 실행합니다.

```bash
./gradlew test
```

`local` profile이나 `.env` 설정이 필요 없고, 위 2단계(`docker compose up`)도 필요하지 않습니다. 개발용 DB(`k-beauty-mysql`)와는 별개 인스턴스라 데이터를 공유하지 않습니다.

단, **Docker 데몬은 실행 중이어야 합니다.** `./gradlew build`도 내부적으로 `test`를 포함하므로 동일합니다. Docker 없이 빌드만 하려면 테스트를 제외합니다.

```bash
./gradlew build -x test
```

반복 실행 시 컨테이너를 재사용해 기동 시간을 줄이려면 `~/.testcontainers.properties`에 다음을 추가합니다.

```properties
testcontainers.reuse.enable=true
```

### 4. 애플리케이션 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

자세한 로컬 DB 연결 방법은 Notion의 로컬 MySQL DB 연결 문서를 참고합니다.

## 협업 가이드

브랜치 전략, PR 제목 규칙, PR 머지 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고합니다.
