package com.xodud1202.springbackend.config;

import com.xodud1202.springbackend.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.xodud1202.springbackend.security.JwtAuthenticationFilter;
import com.xodud1202.springbackend.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailService userDetailsService;
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        // JwtAuthenticationFilter 생성자가 tokenProvider, UserDetailsService 를 받도록 작성되어 있으니,
        // 그대로 넘겨주시면 됩니다.
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }
    
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)  // 추가
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션을 Stateless로 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 인가 규칙 정의
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/hello", "/api", "/auth/**").permitAll()
                                .anyRequest().authenticated()
                )
                // 커스텀 필터 추가
                .addFilterBefore(
                        jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )
        ;
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}