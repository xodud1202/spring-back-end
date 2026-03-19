package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문 고객 결제 기본 정보를 전달합니다.
public class ShopOrderCustomerInfoVO {
	// 고객번호입니다.
	private Long custNo;
	// 고객명입니다.
	private String custNm;
	// 고객 이메일입니다.
	private String email;
	// 고객 연락처입니다.
	private String phoneNumber;
	// Toss 고객 식별키입니다.
	private String customerKey;
	// 현재 결제 디바이스 코드입니다.
	private String deviceGbCd;
	// 고객등급 코드입니다.
	private String custGradeCd;
}
