package com.leets.k_beauty.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 프로필의 CORS 허용 오리진이 실제로 어떤 값으로 풀리는지 검증.
 *
 * <p>application-prod.yaml은 환경변수 뒤에 로컬 주소를 이어 붙여 둔다.
 * 이 방식은 서버의 .env.prod에 CORS_ALLOWED_ORIGINS가 있든 없든
 * 로컬 주소가 목록에서 빠지지 않게 하려는 것인데, 문자열을 이어 붙인 것이라
 * 잘못 풀리면 앱이 뜨고 나서야 알게 된다. 배포 전에 여기서 잡는다.
 *
 * <p>DB를 띄우지 않으려고 설정 파일만 읽는다. 자동 설정을 올리지 않으므로
 * spring.datasource의 ${DB_URL} 같은 값은 아무도 읽지 않는다.
 */
@DisplayName("운영 CORS 허용 오리진")
class ProdCorsOriginsTest {

    private static final String PROPERTY = "cors.allowed-origins";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    @DisplayName("환경변수가 없으면 폴백값 뒤에 로컬 주소가 붙는다")
    void 환경변수가_없으면_폴백값_뒤에_로컬_주소가_붙는다() {
        runner.run(context -> {
            String origins = context.getEnvironment().getProperty(PROPERTY);

            assertThat(origins).isEqualTo(
                    "https://cosmetch.kr,https://www.cosmetch.kr,https://*.cosmetch.kr"
                            + ",http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173");
        });
    }

    @Test
    @DisplayName("환경변수가 있어도 로컬 주소는 목록에서 빠지지 않는다")
    void 환경변수가_있어도_로컬_주소는_목록에서_빠지지_않는다() {
        // 서버 .env.prod에 값이 들어 있는 상황. 폴백값 자리는 이 값으로 대체된다.
        runner.withPropertyValues("CORS_ALLOWED_ORIGINS=https://example.com")
                .run(context -> {
                    String origins = context.getEnvironment().getProperty(PROPERTY);

                    assertThat(origins).isEqualTo(
                            "https://example.com"
                                    + ",http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173");
                });
    }

    @Test
    @DisplayName("SecurityConfig가 받는 List로 쪼갰을 때 로컬 오리진이 개별 항목이 된다")
    void SecurityConfig가_받는_List로_쪼갰을_때_로컬_오리진이_개별_항목이_된다() {
        // SecurityConfig는 이 프로퍼티를 List<String>으로 주입받는다.
        // 쉼표로 제대로 쪼개지지 않으면 오리진 하나가 통째로 안 맞는 패턴이 된다.
        runner.run(context -> {
            String[] origins = context.getEnvironment()
                    .getRequiredProperty(PROPERTY, String[].class);

            assertThat(origins).contains(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "https://*.cosmetch.kr");
        });
    }
}