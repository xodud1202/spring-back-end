package com.xodud1202.springbackend.domain.admin.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xodud1202.springbackend.entity.UserBaseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
// 백오피스 인증 API의 응답 필드를 타입 안전하게 전달합니다.
public record AdminAuthResponse(
	String result,
	String resultMsg,
	String accessToken,
	String usrNo,
	UserBaseEntity userInfo
) {
	// 로그인 성공 응답을 생성합니다.
	public static AdminAuthResponse loginSuccess(String accessToken, UserBaseEntity userInfo) {
		return new AdminAuthResponse("OK", null, accessToken, null, userInfo);
	}

	// 액세스 토큰 확인 또는 재발급 성공 응답을 생성합니다.
	public static AdminAuthResponse accessTokenSuccess(String accessToken, Long usrNo) {
		return new AdminAuthResponse("OK", "OK", accessToken, usrNo == null ? null : String.valueOf(usrNo), null);
	}

	// 로그아웃 성공 응답을 생성합니다.
	public static AdminAuthResponse logoutSuccess() {
		return new AdminAuthResponse("OK", "로그아웃 처리되었습니다.", null, null, null);
	}

	// 인증 실패 응답을 생성합니다.
	public static AdminAuthResponse failure(String result, String resultMsg) {
		return new AdminAuthResponse(result, resultMsg, null, null, null);
	}
}
