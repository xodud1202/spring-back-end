package com.xodud1202.springbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          .csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth -> auth
            // /hello, /api, /api/** 전체를 인증 없이 허용
            .requestMatchers("/hello", "/api", "/api/**", "/backoffice/login").permitAll()
            // 그 외 모든 요청은 인증 필요
            .anyRequest().authenticated()
          )
          .formLogin(Customizer.withDefaults());

        return http.build();
    }
}