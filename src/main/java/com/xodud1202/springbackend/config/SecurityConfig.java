package com.xodud1202.springbackend.config;

import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.config.properties.SecurityCsrfProperties;
import com.xodud1202.springbackend.security.CookieCsrfOriginFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(SecurityCsrfProperties.class)
@RequiredArgsConstructor
// 애플리케이션 보안 필터 체인을 구성합니다.
public class SecurityConfig {

	private static final String[] PUBLIC_PATTERNS = {
		"/hello",
		"/shop/login",
		"/api/backoffice/login",
		"/api/backoffice/logout",
		"/api/token/backoffice/access-token",
		"/api/work/auth/**",
		"/api/snippet/auth/**"
	};
	private static final String[] ADMIN_AUTH_REQUIRED_PATTERNS = {
		"/api/admin/**",
		"/api/backoffice/**",
		"/api/upload/image",
		"/api/upload/education-logo",
		"/api/upload/brand-logo",
		"/api/news/refresh/file"
	};
	private static final String[] CONTROLLER_AUTH_OR_PUBLIC_PATTERNS = {
		"/api/shop/**",
		"/api/work/**",
		"/api/snippet/**",
		"/api/news/**",
		"/api/resume/**",
		"/api/notion/webhook",
		"/api/upload/editor-image"
	};

	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailService userDetailsService;
	private final SecurityCsrfProperties securityCsrfProperties;

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		// JWT 인증 필터를 의존성 주입 기반으로 생성합니다.
		return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		// 관리자 JWT와 각 도메인 세션을 함께 사용하는 보안 필터 체인을 구성합니다.
		http.userDetailsService(userDetailsService)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(PUBLIC_PATTERNS).permitAll()
				.requestMatchers(ADMIN_AUTH_REQUIRED_PATTERNS).authenticated()
				.requestMatchers(CONTROLLER_AUTH_OR_PUBLIC_PATTERNS).permitAll()
				.anyRequest().denyAll()
			)
			.addFilterBefore(new CookieCsrfOriginFilter(securityCsrfProperties), UsernamePasswordAuthenticationFilter.class)
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
