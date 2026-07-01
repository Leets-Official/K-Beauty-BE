# K-Beauty-BE

K-Beauty 서비스의 백엔드 API 서버입니다.

## 기술 스택

- Spring Boot 3.5.16

## 개발 환경

- Java 17
- Gradle Wrapper 8.14.5
- H2 Database
- MySQL Driver

## 데이터베이스

현재 로컬 환경은 H2 인메모리 데이터베이스를 사용합니다.

- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:kbeauty;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: 없음

MySQL 드라이버는 의존성에 포함되어 있으며, 실제 운영/개발 DB 설정은 추후 환경에 맞게 분리할 예정입니다.

## 협업 가이드

브랜치 전략, 이슈/PR 제목 규칙, PR 머지 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고합니다.

