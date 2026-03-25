package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss")
// Toss 결제 연동 설정을 타입 안전하게 전달합니다.
public record TossProperties(
	String clientKey,
	String secretKey
) {
}
