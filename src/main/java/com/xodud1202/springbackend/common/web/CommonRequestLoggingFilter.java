package com.xodud1202.springbackend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// 모든 백엔드 요청의 진입 정보를 공통 포맷으로 기록합니다.
public class CommonRequestLoggingFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(CommonRequestLoggingFilter.class);
	private static final List<String> CLIENT_IP_HEADER_LIST = List.of(
		"X-Forwarded-For",
		"X-Real-IP",
		"Proxy-Client-IP",
		"WL-Proxy-Client-IP"
	);

	@Override
	// 비동기 재디스패치에는 중복 로그를 남기지 않습니다.
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	@Override
	// 에러 재디스패치에는 중복 로그를 남기지 않습니다.
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}

	@Override
	// 요청 처리 전후 시간을 측정하고 공통 접근 로그를 남깁니다.
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		long startedAt = System.nanoTime();
		try {
			// 실제 요청 처리는 다음 필터와 컨트롤러에 위임합니다.
			filterChain.doFilter(request, response);
		} finally {
			// 응답 상태와 처리시간을 함께 기록해 운영 추적성을 확보합니다.
			long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
			log.info(
				"HTTP 요청 ip={} method={} path={} status={} durationMs={}",
				resolveClientIp(request),
				request == null ? "" : safeValue(request.getMethod()),
				buildLoggedRequestTarget(request),
				response == null ? 0 : response.getStatus(),
				durationMs
			);
		}
	}

	// 프록시 헤더를 우선 사용해 실제 클라이언트 IP를 복원합니다.
	String resolveClientIp(HttpServletRequest request) {
		// 역프록시 헤더가 있으면 첫 번째 실제 IP를 사용합니다.
		for (String headerName : CLIENT_IP_HEADER_LIST) {
			String headerIp = extractClientIpFromHeaderValue(request == null ? null : request.getHeader(headerName));
			if (!headerIp.isEmpty()) {
				return headerIp;
			}
		}

		// 프록시 헤더가 없으면 서블릿이 인식한 원격 주소를 사용합니다.
		return request == null ? "" : safeValue(request.getRemoteAddr());
	}

	// 요청 URI와 마스킹된 쿼리스트링을 합쳐 로그용 경로 문자열을 만듭니다.
	String buildLoggedRequestTarget(HttpServletRequest request) {
		// 요청 자체가 없으면 빈 문자열을 반환합니다.
		if (request == null) {
			return "";
		}

		// 경로는 그대로 두고 쿼리값만 마스킹합니다.
		String requestUri = safeValue(request.getRequestURI());
		String sanitizedQueryString = sanitizeQueryString(request.getQueryString());
		if (sanitizedQueryString.isEmpty()) {
			return requestUri;
		}
		return requestUri + "?" + sanitizedQueryString;
	}

	// 전달 헤더 문자열에서 첫 번째 유효 IP만 추출합니다.
	private String extractClientIpFromHeaderValue(String headerValue) {
		// 헤더가 비어 있으면 사용할 IP가 없습니다.
		if (headerValue == null || headerValue.isBlank()) {
			return "";
		}

		// X-Forwarded-For는 다중 IP가 올 수 있어 첫 번째 유효 값만 사용합니다.
		for (String token : headerValue.split(",")) {
			String normalizedToken = token == null ? "" : token.trim();
			if (normalizedToken.isEmpty() || "unknown".equalsIgnoreCase(normalizedToken)) {
				continue;
			}
			return normalizedToken;
		}
		return "";
	}

	// 쿼리스트링 값을 모두 마스킹해 로그용 문자열로 정리합니다.
	private String sanitizeQueryString(String queryString) {
		// 쿼리스트링이 없으면 빈 문자열을 반환합니다.
		if (queryString == null || queryString.isBlank()) {
			return "";
		}

		// 각 쿼리 항목의 값만 가리고 키는 유지합니다.
		List<String> sanitizedTokenList = new ArrayList<>();
		for (String queryToken : queryString.split("&")) {
			String sanitizedToken = sanitizeQueryToken(queryToken);
			if (!sanitizedToken.isEmpty()) {
				sanitizedTokenList.add(sanitizedToken);
			}
		}
		return String.join("&", sanitizedTokenList);
	}

	// 개별 쿼리 항목에서 값만 마스킹하고 키는 유지합니다.
	private String sanitizeQueryToken(String queryToken) {
		// 빈 토큰은 로그에 남기지 않습니다.
		if (queryToken == null || queryToken.isBlank()) {
			return "";
		}

		// key=value 형태면 value만 마스킹하고 key만 있는 경우는 그대로 남깁니다.
		int separatorIndex = queryToken.indexOf('=');
		if (separatorIndex < 0) {
			return queryToken.trim();
		}
		String key = queryToken.substring(0, separatorIndex).trim();
		return key.isEmpty() ? "" : key + "=*";
	}

	// null 문자열을 빈 문자열로 보정합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
