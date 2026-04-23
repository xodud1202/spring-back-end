package com.xodud1202.springbackend.domain.shop.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
// 쇼핑몰 현재 로그인 상태 응답 정보를 전달합니다.
public record ShopAuthMeResponse(
	boolean authenticated,
	Long custNo,
	String custNm,
	String custGradeCd,
	String custGradeNm
) {
	// 비로그인 상태 응답을 생성합니다.
	public static ShopAuthMeResponse unauthenticated() {
		return new ShopAuthMeResponse(false, null, null, null, null);
	}

	// 로그인 상태 응답을 생성합니다.
	public static ShopAuthMeResponse authenticated(
		Long custNo,
		String custNm,
		String custGradeCd,
		String custGradeNm
	) {
		return new ShopAuthMeResponse(true, custNo, custNm, custGradeCd, custGradeNm);
	}
}
