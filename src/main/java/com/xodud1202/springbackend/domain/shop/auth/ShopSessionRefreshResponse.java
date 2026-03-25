package com.xodud1202.springbackend.domain.shop.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
// 쇼핑몰 세션 갱신 응답 정보를 전달합니다.
public record ShopSessionRefreshResponse(
	boolean authenticated,
	Long custNo,
	String custNm,
	String custGradeCd,
	String custGradeNm
) {
	// 비로그인 상태 응답을 생성합니다.
	public static ShopSessionRefreshResponse unauthenticated() {
		return new ShopSessionRefreshResponse(false, null, null, null, null);
	}

	// 로그인 상태 응답을 생성합니다.
	public static ShopSessionRefreshResponse authenticated(
		Long custNo,
		String custNm,
		String custGradeCd,
		String custGradeNm
	) {
		return new ShopSessionRefreshResponse(true, custNo, custNm, custGradeCd, custGradeNm);
	}
}
