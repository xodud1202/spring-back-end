package com.xodud1202.springbackend.domain.shop.auth;

import jakarta.validation.constraints.NotBlank;

// 구글 로그인 식별 요청 정보를 전달합니다.
public record ShopGoogleLoginRequest(
	@NotBlank(message = "구글 사용자 식별값을 확인해주세요.") String sub,
	String email,
	String name,
	String picture
) {
}
