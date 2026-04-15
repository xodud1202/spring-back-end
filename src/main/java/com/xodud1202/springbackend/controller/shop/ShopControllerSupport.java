package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.util.CommonTextUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_MO;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_PC;

// 쇼핑몰 컨트롤러 공통 요청 해석 기능을 제공합니다.
abstract class ShopControllerSupport {
	protected static final String COOKIE_CUST_NO = "cust_no";
	protected static final String COOKIE_CUST_GRADE_CD = "cust_grade_cd";

	// 요청 쿠키에서 고객번호를 파싱해 반환합니다.
	protected Long parseCustNoCookie(HttpServletRequest request) {
		// 고객번호 쿠키 값이 없으면 null을 반환합니다.
		String custNoValue = findCookieValue(request, COOKIE_CUST_NO);
		if (custNoValue == null || custNoValue.trim().isEmpty()) {
			return null;
		}

		// 숫자 형식이 아니면 로그인 정보가 없는 것으로 처리합니다.
		try {
			return Long.valueOf(custNoValue.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	// 로그인된 고객번호를 필수로 요구하고 미로그인 시 예외를 발생시킵니다.
	protected Long requireAuthenticatedCustNo(HttpServletRequest request) {
		Long custNo = parseCustNoCookie(request);
		if (custNo == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}
		return custNo;
	}

	// 미인증 요청에 대한 공통 401 응답을 반환합니다.
	protected ResponseEntity<Object> unauthorizedResponse() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
	}

	// 요청 쿠키에서 지정한 이름의 값을 조회합니다.
	protected String findCookieValue(HttpServletRequest request, String cookieName) {
		// 쿠키 목록이나 이름이 없으면 null을 반환합니다.
		if (request == null || request.getCookies() == null || cookieName == null) {
			return null;
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.findFirst()
			.map(Cookie::getValue)
			.orElse(null);
	}

	// URL 인코딩된 쿠키 값을 원문 문자열로 디코딩합니다.
	protected String decodeCookieValue(String value) {
		// 값이 없으면 null을 그대로 반환합니다.
		if (value == null) {
			return null;
		}
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	// 숫자 또는 문자열 값을 정수로 변환합니다.
	protected Integer parseIntegerValue(Object rawValue) {
		// 값이 없으면 null을 반환합니다.
		switch (rawValue) {
			case null -> {
				return null;
			}

			// 숫자 타입은 int 값으로 변환합니다.
			case Number numberValue -> {
				return numberValue.intValue();
			}

			// 문자열 타입은 공백 제거 후 정수 변환을 시도합니다.
			case String stringValue -> {
				String normalizedValue = stringValue.trim();
				if (normalizedValue.isEmpty()) {
					return null;
				}
				try {
					return Integer.valueOf(normalizedValue);
				} catch (NumberFormatException exception) {
					return null;
				}
			}
			default -> {
			}
		}
		return null;
	}

	// 요청 본문 맵에서 필수 문자열 값을 trim 처리해 반환합니다.
	protected String requireRequestBodyTextValue(Map<String, Object> requestBody, String fieldName, String message) {
		Object rawValue = requestBody == null ? null : requestBody.get(fieldName);
		String normalizedValue = rawValue instanceof String ? ((String) rawValue).trim() : "";
		if (normalizedValue.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return normalizedValue;
	}

	// 요청 본문 값에서 1 이상의 필수 정수 값을 반환합니다.
	protected Integer requirePositiveIntegerValue(Object rawValue, String message) {
		Integer parsedValue = parseIntegerValue(rawValue);
		if (parsedValue == null || parsedValue < 1) {
			throw new IllegalArgumentException(message);
		}
		return parsedValue;
	}

	// 선택 기획전 번호를 양수 정수 또는 null로 정규화합니다.
	protected Integer normalizeOptionalExhibitionNo(Object rawValue) {
		// 숫자/문자열 값을 정수로 바꾼 뒤 1 이상만 허용합니다.
		Integer parsedValue = parseIntegerValue(rawValue);
		return parsedValue == null || parsedValue < 1 ? null : parsedValue;
	}

	// 요청 헤더를 기준으로 결제 디바이스 코드를 반환합니다.
	protected String resolveDeviceGbCd(HttpServletRequest request) {
		// 모바일 User-Agent면 MO, 그 외는 PC로 판단합니다.
		String userAgent = request == null ? null : request.getHeader("User-Agent");
		if (userAgent == null) {
			return DEVICE_GB_PC;
		}
		String normalizedUserAgent = userAgent.toLowerCase();
		if (normalizedUserAgent.contains("android")
			|| normalizedUserAgent.contains("iphone")
			|| normalizedUserAgent.contains("ipad")
			|| normalizedUserAgent.contains("mobile")) {
			return DEVICE_GB_MO;
		}
		return DEVICE_GB_PC;
	}

	// 요청 헤더를 기준으로 프론트 절대 Origin 값을 추론합니다.
	protected String resolveShopOrigin(HttpServletRequest request) {
		// Origin, X-Forwarded, Referer 순으로 프론트 Origin을 추론합니다.
		String origin = trimToNull(request == null ? null : request.getHeader("Origin"));
		if (origin != null) {
			return origin;
		}
		String forwardedHost = trimToNull(request == null ? null : request.getHeader("X-Forwarded-Host"));
		String forwardedProto = trimToNull(request == null ? null : request.getHeader("X-Forwarded-Proto"));
		if (forwardedHost != null) {
			return (forwardedProto == null ? "http" : forwardedProto) + "://" + forwardedHost;
		}
		String referer = trimToNull(request == null ? null : request.getHeader("Referer"));
		if (referer != null) {
			int slashIndex = referer.indexOf('/', referer.indexOf("://") + 3);
			return slashIndex > -1 ? referer.substring(0, slashIndex) : referer;
		}
		if (request == null) {
			return "";
		}
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	protected String trimToNull(String value) {
		return CommonTextUtils.trimToNull(value);
	}
}
