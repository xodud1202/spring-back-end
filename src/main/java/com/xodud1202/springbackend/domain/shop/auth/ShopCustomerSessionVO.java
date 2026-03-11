package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 쇼핑몰 고객 로그인 세션 정보를 전달합니다.
public class ShopCustomerSessionVO {
	// 고객 번호입니다.
	private Long custNo;
	// 로그인 아이디입니다.
	private String loginId;
	// 고객명입니다.
	private String custNm;
	// 고객 등급 코드입니다.
	private String custGradeCd;
	// CI 값입니다.
	private String ci;
	// 이메일입니다.
	private String email;
}
