package com.xodud1202.springbackend.domain.shop.auth;

// 쇼핑몰 고객 로그인 세션 정보를 전달합니다.
public record ShopCustomerSessionVO(
	Long custNo,
	String loginId,
	String custNm,
	String custGradeCd,
	String ci,
	String email
) {
}
