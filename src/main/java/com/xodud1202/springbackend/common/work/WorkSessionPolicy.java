package com.xodud1202.springbackend.common.work;

import jakarta.servlet.http.HttpSession;

import java.time.Duration;

// 업무관리 로그인 세션과 쿠키 정책 상수를 중앙화합니다.
public final class WorkSessionPolicy {
	public static final String COOKIE_WORK_USER_NO = "work_user_no";
	public static final String SESSION_ATTR_WORK_USER_NO = "workUserNo";
	public static final int SESSION_TIMEOUT_SECONDS = 60 * 60 * 5;
	public static final Duration SESSION_COOKIE_MAX_AGE = Duration.ofHours(5);

	// 유틸리티 클래스 인스턴스 생성을 방지합니다.
	private WorkSessionPolicy() {
		throw new UnsupportedOperationException("업무관리 세션 정책 클래스는 인스턴스화할 수 없습니다.");
	}

	// 세션 속성 값을 업무관리 사용자번호 Long 값으로 변환합니다.
	public static Long resolveWorkUserNo(Object sessionValue) {
		// 저장 타입이 달라도 현재 로그인 사용자번호를 일관되게 읽을 수 있도록 처리합니다.
		if (sessionValue instanceof Long workUserNo && workUserNo > 0L) {
			return workUserNo;
		}
		if (sessionValue instanceof Integer workUserNo && workUserNo > 0) {
			return workUserNo.longValue();
		}
		if (sessionValue instanceof String workUserNoText) {
			try {
				Long workUserNo = Long.valueOf(workUserNoText);
				if (workUserNo > 0L) {
					return workUserNo;
				}
			} catch (NumberFormatException ignored) {
				// 문자열 파싱 실패는 null 반환으로 처리합니다.
			}
		}
		return null;
	}

	// 업무관리 세션의 비활성 타임아웃을 정책 시간으로 갱신합니다.
	public static void refreshSessionTimeout(HttpSession session) {
		// 로그인 유지시간 5시간 기준으로 세션 만료시간을 다시 맞춥니다.
		session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
	}
}
