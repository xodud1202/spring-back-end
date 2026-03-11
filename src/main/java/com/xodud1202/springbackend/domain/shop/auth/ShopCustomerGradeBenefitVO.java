package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 고객등급별 가입 혜택 정보를 전달합니다.
public class ShopCustomerGradeBenefitVO {
	// 고객등급 코드입니다.
	private String custGradeCd;
	// 상품 쿠폰 번호입니다.
	private Long goodsCpnNo;
	// 상품 쿠폰 지급 수량입니다.
	private Integer goodsCpnCnt;
	// 장바구니 쿠폰 번호입니다.
	private Long cartCpnNo;
	// 장바구니 쿠폰 지급 수량입니다.
	private Integer cartCpnCnt;
	// 배송비 쿠폰 번호입니다.
	private Long deliveryCpnNo;
	// 배송비 쿠폰 지급 수량입니다.
	private Integer deliveryCpnCnt;
}
