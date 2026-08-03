package com.leets.k_beauty.global.config;

import org.springframework.context.annotation.Bean;                                                                                                                                                      import org.springframework.context.annotation.Configuration;                                                                                                                                             import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        // TODO: X-Session-Token 검증을 Security Filter로 분리한 뒤 authenticated()로 변경
                        .anyRequest().permitAll());

        return http.build();
    }
}