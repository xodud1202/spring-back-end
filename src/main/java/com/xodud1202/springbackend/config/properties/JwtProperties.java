package com.xodud1202.springbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "jwt")
// JWT 관련 설정을 타입 안전하게 전달합니다.
public record JwtProperties(
	String secret,
	@DurationUnit(ChronoUnit.MILLIS) Duration accessTokenExpiration,
	@DurationUnit(ChronoUnit.MILLIS) Duration refreshTokenExpiration,
	boolean cookieSecure
) {
}
