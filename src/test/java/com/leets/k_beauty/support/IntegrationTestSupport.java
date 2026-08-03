package com.leets.k_beauty.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 스프링 컨텍스트 전체를 띄우는 테스트의 공통 기반.
 *
 * <p>주의: SurveyDataInitializer는 ApplicationRunner라서 @SpringBootTest에서는 실행되지만
 * @DataJpaTest 같은 슬라이스 테스트에서는 실행되지 않는다. 슬라이스용 시드는 별도로 준비해야 한다.
 *
 * <p>순수 로직 단위 테스트는 이 클래스를 상속하지 말 것. 컨테이너 기동 비용만 늘어난다.
 */
@SpringBootTest
@Import(MySqlTestContainerConfig.class)
public abstract class IntegrationTestSupport {
}