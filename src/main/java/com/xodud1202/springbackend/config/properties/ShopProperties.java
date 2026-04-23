package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shop")
// 쇼핑몰 프론트 연동 설정을 타입 안전하게 전달합니다.
public record ShopProperties(
	String frontBaseUrl
) {
}
