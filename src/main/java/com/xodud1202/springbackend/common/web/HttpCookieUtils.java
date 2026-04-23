package com.xodud1202.springbackend.common.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// HTTP 요청/응답 쿠키 문자열 처리 공용 기능을 제공합니다.
public final class HttpCookieUtils {
	// 유틸리티 클래스 인스턴스 생성을 방지합니다.
	private HttpCookieUtils() {
		throw new UnsupportedOperationException("HTTP 쿠키 유틸리티 클래스는 인스턴스화할 수 없습니다.");
	}

	// 요청 쿠키에서 지정한 이름의 값을 조회합니다.
	public static String findCookieValue(HttpServletRequest request, String cookieName) {
		// 요청이나 쿠키명이 없으면 null을 반환합니다.
		if (request == null || request.getCookies() == null || cookieName == null) {
			return null;
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.findFirst()
			.map(Cookie::getValue)
			.orElse(null);
	}

	// 쿠키에 저장 가능한 문자열로 URL 인코딩합니다.
	public static String encodeCookieValue(String value) {
		// null 값은 빈 문자열로 저장합니다.
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	// URL 인코딩된 쿠키 문자열을 원래 값으로 복원합니다.
	public static String decodeCookieValue(String value) {
		// null 값은 빈 문자열로 복원합니다.
		return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
	}
}
