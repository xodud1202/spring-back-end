package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 배송비 결제 승인 요청 데이터를 전달합니다.
public class ShopOrderExchangePaymentConfirmPO {
	// 결제번호입니다.
	private Long payNo;
	// 클레임번호입니다.
	private String clmNo;
	// Toss 결제키입니다.
	private String paymentKey;
	// 승인 요청 금액입니다.
	private Long amount;
}
