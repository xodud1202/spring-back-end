package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
// 인증 관련 httpOnly 쿠키 생성 규칙을 중앙화합니다.
public class AuthCookieFactory {
	private static final Duration SHOP_SESSION_COOKIE_MAX_AGE = Duration.ofHours(1);

	private final JwtProperties jwtProperties;

	// 백오피스 리프레시 토큰 쿠키를 생성합니다.
	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(jwtProperties.refreshTokenExpiration())
			.sameSite("Strict")
			.build();
	}

	// 백오피스 리프레시 토큰 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredRefreshTokenCookie() {
		return ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Strict")
			.build();
	}

	// 쇼핑몰 로그인 쿠키를 생성합니다.
	public ResponseCookie createShopLoginCookie(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(SHOP_SESSION_COOKIE_MAX_AGE)
			.sameSite("Lax")
			.build();
	}

	// 쇼핑몰 로그인 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredShopLoginCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Lax")
			.build();
	}
}
