package com.xodud1202.springbackend.controller.snippet;

import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

// 인증된 스니펫 API 호출 시 세션과 로그인 쿠키 만료시간을 연장합니다.
@Component
@RequiredArgsConstructor
public class SnippetSessionExtendInterceptor implements HandlerInterceptor {
	private final AuthCookieFactory authCookieFactory;
	private final SignedLoginTokenService signedLoginTokenService;

	@Override
	// 성공적으로 처리된 스니펫 요청에 대해 세션과 쿠키 유지시간을 다시 갱신합니다.
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
		// 비로그인 요청이나 비성공 응답은 연장하지 않습니다.
		if (!isSuccessStatus(response.getStatus())) {
			return;
		}

		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}

		Long snippetUserNo = SnippetSessionPolicy.resolveSnippetUserNo(session.getAttribute(SnippetSessionPolicy.SESSION_ATTR_SNIPPET_USER_NO));
		if (snippetUserNo == null) {
			return;
		}

		// 성공적으로 인증된 요청은 세션과 쿠키를 모두 5시간 기준으로 다시 맞춥니다.
		SnippetSessionPolicy.refreshSessionTimeout(session);
		response.addHeader(
			HttpHeaders.SET_COOKIE,
			authCookieFactory.createSnippetLoginCookie(
				SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO,
				signedLoginTokenService.generateSnippetAuthToken(snippetUserNo)
			).toString()
		);
	}

	// 응답 상태가 성공 범위인지 확인합니다.
	private boolean isSuccessStatus(int statusCode) {
		// 2xx 응답만 슬라이딩 만료 대상으로 인정합니다.
		return statusCode >= 200 && statusCode < 300;
	}
}
