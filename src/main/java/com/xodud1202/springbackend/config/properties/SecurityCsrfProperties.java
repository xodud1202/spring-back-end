package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security.csrf")
// 쿠키 인증 API의 CSRF Origin 검증 설정을 타입 안전하게 전달합니다.
public record SecurityCsrfProperties(
	List<String> allowedOrigins
) {
	// 설정된 허용 Origin 목록을 null 안전하게 반환합니다.
	public List<String> safeAllowedOrigins() {
		return allowedOrigins == null ? List.of() : allowedOrigins;
	}
}
