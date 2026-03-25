package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "juso")
// 주소 검색 API 설정을 타입 안전하게 전달합니다.
public record JusoProperties(
	String apiKey
) {
}
