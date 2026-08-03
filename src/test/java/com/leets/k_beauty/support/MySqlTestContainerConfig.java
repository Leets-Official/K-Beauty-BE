package com.leets.k_beauty.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * 테스트용 MySQL 컨테이너 설정.
 * 운영과 동일한 이미지·문자셋을 사용한다 (docker-compose.yml 기준).
 * 개발용 컨테이너(k-beauty-mysql)와는 별개 인스턴스라 mysql-data 볼륨에 영향을 주지 않는다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainerConfig {

    // @ServiceConnection이 컨테이너의 접속 정보를 datasource 프로퍼티로 주입한다.
    // 덕분에 테스트 설정에 ${DB_URL} 같은 환경변수 참조가 필요 없다.
    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4")
                .withCommand("--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci")
                .withReuse(true);
    }
}