package com.xodud1202.springbackend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
			// 본문 값은 로그에서 제외하고 응답 상태와 처리시간을 함께 기록합니다.
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

	// 요청 URI와 원본 쿼리스트링을 합쳐 로그용 경로 문자열을 만듭니다.
	String buildLoggedRequestTarget(HttpServletRequest request) {
		// 요청 자체가 없으면 빈 문자열을 반환합니다.
		if (request == null) {
			return "";
		}

		// 쿼리값과 인코딩을 변경하지 않고 요청받은 문자열 그대로 기록합니다.
		String requestUri = safeValue(request.getRequestURI());
		String queryString = request.getQueryString();
		if (queryString == null || queryString.isBlank()) {
			return requestUri;
		}
		return requestUri + "?" + queryString;
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

	// null 문자열을 빈 문자열로 보정합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
