package com.xodud1202.springbackend.domain.work;

// 업무관리 세션 복구 응답 정보를 전달합니다.
public record WorkSessionRefreshResponse(
	boolean authenticated,
	Long workUserNo,
	String loginId,
	String userNm
) {
	// 비로그인 응답 객체를 생성합니다.
	public static WorkSessionRefreshResponse unauthenticated() {
		return new WorkSessionRefreshResponse(false, null, "", "");
	}

	// 로그인 복구 성공 응답 객체를 생성합니다.
	public static WorkSessionRefreshResponse authenticated(Long workUserNo, String loginId, String userNm) {
		return new WorkSessionRefreshResponse(true, workUserNo, loginId, userNm);
	}
}
