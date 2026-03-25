package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notion")
// Notion API 및 웹훅 설정을 타입 안전하게 전달합니다.
public record NotionProperties(
	String apiKey,
	String apiVersion,
	String webhookVerificationToken
) {
}
