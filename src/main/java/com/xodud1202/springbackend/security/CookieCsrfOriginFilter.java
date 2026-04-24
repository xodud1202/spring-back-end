package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.common.web.HttpOriginUtils;
import com.xodud1202.springbackend.config.properties.SecurityCsrfProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 쿠키 기반 인증 API의 상태 변경 요청에 Origin/Referer 기반 CSRF 검증을 수행합니다.
public class CookieCsrfOriginFilter extends OncePerRequestFilter {
	private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final List<String> COOKIE_AUTH_API_PREFIXES = List.of("/api/shop/", "/api/work/", "/api/snippet/");
	private static final String EDITOR_IMAGE_UPLOAD_PATH = "/api/upload/editor-image";

	private final SecurityCsrfProperties securityCsrfProperties;

	// CSRF Origin 허용 설정을 주입받습니다.
	public CookieCsrfOriginFilter(SecurityCsrfProperties securityCsrfProperties) {
		this.securityCsrfProperties = securityCsrfProperties;
	}

	@Override
	// 보호 대상 쿠키 요청이면 요청 Origin을 검증하고, 아니면 다음 필터로 전달합니다.
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		// 보호 대상이 아니거나 쿠키가 없는 요청은 기존 동작을 유지합니다.
		if (!requiresCsrfOriginCheck(request) || !hasCookieHeader(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		// 허용된 Origin/Referer에서 온 쿠키 요청만 상태 변경 API로 전달합니다.
		if (isAllowedRequestOrigin(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Origin 검증에 실패했습니다.");
	}

	// 현재 요청이 CSRF Origin 검증 대상인지 확인합니다.
	private boolean requiresCsrfOriginCheck(HttpServletRequest request) {
		// 안전하지 않은 HTTP 메서드만 검사합니다.
		String method = request == null ? null : request.getMethod();
		if (method == null || !UNSAFE_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
			return false;
		}

		// 쿠키 인증을 사용하는 공개 컨트롤러 인증 API만 검사합니다.
		String path = normalizeRequestPath(request);
		if (EDITOR_IMAGE_UPLOAD_PATH.equals(path)) {
			return true;
		}
		return COOKIE_AUTH_API_PREFIXES.stream().anyMatch(path::startsWith);
	}

	// 요청에 Cookie 헤더가 포함되어 있는지 확인합니다.
	private boolean hasCookieHeader(HttpServletRequest request) {
		String cookieHeader = request == null ? null : request.getHeader(HttpHeaders.COOKIE);
		return cookieHeader != null && !cookieHeader.isBlank();
	}

	// Origin 또는 Referer가 허용 목록에 포함되는지 확인합니다.
	private boolean isAllowedRequestOrigin(HttpServletRequest request) {
		// 요청 Origin/Referer가 허용 목록에 포함될 때만 상태 변경을 허용합니다.
		return HttpOriginUtils.resolveAllowedRequestOrigin(request, securityCsrfProperties.safeAllowedOrigins()) != null;
	}

	// 요청 경로에서 컨텍스트 경로를 제거해 보안 매칭 기준 경로로 변환합니다.
	private String normalizeRequestPath(HttpServletRequest request) {
		String requestUri = request == null ? "" : request.getRequestURI();
		String contextPath = request == null ? "" : request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}
		return requestUri;
	}
}
