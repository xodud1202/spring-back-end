package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 배송비 결제 대상 클레임 정보를 전달합니다.
public class ShopOrderExchangePaymentClaimVO {
	// 클레임번호입니다.
	private String clmNo;
	// 원 주문번호입니다.
	private String ordNo;
	// 클레임 진행 상태 코드입니다.
	private String chgStatCd;
	// 교환 배송비입니다.
	private Integer payDelvAmt;
	// 클레임 상세 최소 상태 코드입니다.
	private String minChgDtlStatCd;
	// 클레임 상세 최대 상태 코드입니다.
	private String maxChgDtlStatCd;
}
