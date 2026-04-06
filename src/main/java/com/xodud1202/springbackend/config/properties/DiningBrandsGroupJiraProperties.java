package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dining.brands.group.jira")
// 다이닝브랜즈그룹 Jira API 인증 설정을 타입 안전하게 전달합니다.
public record DiningBrandsGroupJiraProperties(
	String email,
	String token
) {
}
