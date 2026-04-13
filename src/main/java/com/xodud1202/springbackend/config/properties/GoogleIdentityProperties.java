package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.identity")
// 구글 GIS 검증 설정을 타입 안전하게 전달합니다.
public record GoogleIdentityProperties(
	String clientId
) {
}
