package com.leets.k_beauty.global.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    // Swagger UI와 API 문서 경로.
    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 기본 로그인 화면 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // 4. 세션을 만들지 않는다. 사용자 식별은 X-Session-Token 헤더로만 한다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. URL별 접근 권한
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        // API 문서
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        // TODO: X-Session-Token 검증을 Security Filter로 분리한 뒤 /api/**를 authenticated()로 변경
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll());

        return http.build();
    }

    /**
     * CORS 설정.
     *
     * <p>모든 API가 X-Session-Token 커스텀 헤더를 요구해서 단순 요청 조건을 벗어나므로,
     * 브라우저는 매 요청마다 preflight(OPTIONS)를 먼저 보낸다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 서브도메인 와일드카드(https://*.cosmetch.kr)를 쓸 수 있고,
        // allowCredentials가 true여도 정상 동작
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 프론트가 보내는 요청 헤더를 전부 허용한다.
        configuration.setAllowedHeaders(List.of("*"));
        // 프론트가 응답에서 직접 읽어야 하는 헤더. 기본적으로는 브라우저가 감춘다.
        configuration.setExposedHeaders(List.of(SESSION_TOKEN_HEADER, "Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}