package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 신청 결과를 전달합니다.
public class ShopOrderExchangeResultVO {
	// 클레임번호입니다.
	private String clmNo;
	// 원 주문번호입니다.
	private String ordNo;
	// 교환 배송비 결제 필요 여부입니다.
	private Boolean paymentRequiredYn;
	// 교환 배송비입니다.
	private Long payDelvAmt;
	// Toss 결제 준비 정보입니다.
	private ShopOrderPaymentPrepareVO paymentPrepare;
}
