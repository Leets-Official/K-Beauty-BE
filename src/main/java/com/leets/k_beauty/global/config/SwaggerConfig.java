package com.leets.k_beauty.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * Swagger 문서 기본 정보.
     */
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Cosmetch API")
                .description("""
                        K-Beauty 화장품 추천 서비스 API
                        """)
                .version("v1");

        return new OpenAPI().info(info);
    }
}