package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 구글 로그인 식별 요청 정보를 전달합니다.
public class ShopGoogleLoginRequest {
	// 구글 사용자 고유 식별자입니다.
	private String sub;
	// 구글 이메일입니다.
	private String email;
	// 구글 표시 이름입니다.
	private String name;
	// 구글 프로필 이미지 URL입니다.
	private String picture;
}
