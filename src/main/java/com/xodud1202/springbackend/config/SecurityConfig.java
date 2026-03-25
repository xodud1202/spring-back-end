package com.xodud1202.springbackend.config;

import com.xodud1202.springbackend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// 애플리케이션 보안 필터 체인을 구성합니다.
public class SecurityConfig {

	private static final String[] PERMIT_ALL_PATTERNS = {"/hello", "/api/**", "/shop/login"};

	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailService userDetailsService;

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		// JWT 인증 필터를 의존성 주입 기반으로 생성합니다.
		return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		// 세션 없는 JWT 기반 API 보안 필터 체인을 구성합니다.
		http.userDetailsService(userDetailsService)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize ->
				authorize.requestMatchers(PERMIT_ALL_PATTERNS).permitAll().anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 비밀번호 인코더를 BCrypt로 제공합니다.
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		// Spring Security 인증 매니저를 외부에서 주입 가능하게 노출합니다.
		return authenticationConfiguration.getAuthenticationManager();
	}
}
