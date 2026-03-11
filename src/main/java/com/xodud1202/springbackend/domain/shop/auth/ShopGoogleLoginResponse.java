package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 구글 로그인 판정 결과를 전달합니다.
public class ShopGoogleLoginResponse {
	// 기존 회원 로그인 성공 여부입니다.
	private boolean loginSuccess;
	// 추가 정보 입력 필요 여부입니다.
	private boolean joinRequired;
	// 고객 번호입니다.
	private Long custNo;
	// 고객명입니다.
	private String custNm;
	// 고객 등급 코드입니다.
	private String custGradeCd;
	// 추천 로그인 아이디입니다.
	private String loginId;
	// 처리 결과 메시지입니다.
	private String message;
}
