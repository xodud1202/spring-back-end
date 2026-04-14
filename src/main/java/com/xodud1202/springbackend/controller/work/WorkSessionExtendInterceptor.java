package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

// 인증된 업무관리 API 호출 시 세션과 로그인 쿠키 만료시간을 연장합니다.
@Component
@RequiredArgsConstructor
public class WorkSessionExtendInterceptor implements HandlerInterceptor {
	private final AuthCookieFactory authCookieFactory;

	@Override
	// 성공적으로 처리된 업무관리 요청에 대해 세션과 쿠키 유지시간을 다시 갱신합니다.
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
		// 비로그인 요청이나 비성공 응답은 연장하지 않습니다.
		if (!isSuccessStatus(response.getStatus())) {
			return;
		}

		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}

		Long workUserNo = WorkSessionPolicy.resolveWorkUserNo(session.getAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO));
		if (workUserNo == null) {
			return;
		}

		// 성공적으로 인증된 요청은 세션과 쿠키를 모두 5시간 기준으로 다시 맞춥니다.
		WorkSessionPolicy.refreshSessionTimeout(session);
		response.addHeader(
			HttpHeaders.SET_COOKIE,
			authCookieFactory.createWorkLoginCookie(
				WorkSessionPolicy.COOKIE_WORK_USER_NO,
				String.valueOf(workUserNo)
			).toString()
		);
	}

	// 응답 상태가 성공 범위인지 확인합니다.
	private boolean isSuccessStatus(int statusCode) {
		// 2xx 응답만 슬라이딩 만료 대상으로 인정합니다.
		return statusCode >= 200 && statusCode < 300;
	}
}
