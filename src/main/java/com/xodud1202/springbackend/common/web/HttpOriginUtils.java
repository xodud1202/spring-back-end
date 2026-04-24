package com.xodud1202.springbackend.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.util.List;
import java.util.Locale;

// HTTP Origin/Referer 문자열 정규화와 허용 Origin 판정을 공용화합니다.
public final class HttpOriginUtils {
	// 유틸리티 클래스 인스턴스 생성을 방지합니다.
	private HttpOriginUtils() {
		throw new UnsupportedOperationException("HTTP Origin 유틸리티 클래스는 인스턴스화할 수 없습니다.");
	}

	// Origin 또는 URL 문자열을 scheme://host[:port] 형태로 정규화합니다.
	public static String normalizeOrigin(String value) {
		// 비어 있는 값은 Origin으로 사용할 수 없습니다.
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			URI uri = URI.create(value.trim());
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (scheme == null || host == null) {
				return null;
			}

			// 비교 안정성을 위해 scheme/host는 소문자로 정규화합니다.
			String normalizedOrigin = scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
			if (uri.getPort() >= 0) {
				normalizedOrigin += ":" + uri.getPort();
			}
			return normalizedOrigin;
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	// 요청의 Origin 헤더를 우선하고 없으면 Referer 헤더에서 Origin을 복원합니다.
	public static String resolveRequestOrigin(HttpServletRequest request) {
		// Origin 헤더가 있으면 우선 사용합니다.
		String originHeader = request == null ? null : request.getHeader(HttpHeaders.ORIGIN);
		String normalizedOrigin = normalizeOrigin(originHeader);
		if (normalizedOrigin != null) {
			return normalizedOrigin;
		}

		// Origin이 없으면 Referer 전체 URL에서 Origin만 추출합니다.
		String refererHeader = request == null ? null : request.getHeader(HttpHeaders.REFERER);
		return normalizeOrigin(refererHeader);
	}

	// 요청 Origin이 허용 목록과 일치하면 정규화된 Origin을 반환합니다.
	public static String resolveAllowedRequestOrigin(HttpServletRequest request, List<String> allowedOriginList) {
		// 현재 요청에서 복원한 Origin이 없으면 허용 여부를 판단할 수 없습니다.
		String requestOrigin = resolveRequestOrigin(request);
		if (requestOrigin == null || allowedOriginList == null || allowedOriginList.isEmpty()) {
			return null;
		}

		// 허용 Origin 목록을 동일 규칙으로 정규화해 완전 일치 여부를 확인합니다.
		for (String allowedOrigin : allowedOriginList) {
			String normalizedAllowedOrigin = normalizeOrigin(allowedOrigin);
			if (requestOrigin.equals(normalizedAllowedOrigin)) {
				return requestOrigin;
			}
		}
		return null;
	}
}
