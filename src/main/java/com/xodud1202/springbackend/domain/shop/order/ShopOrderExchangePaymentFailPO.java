package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 배송비 결제 실패 요청 데이터를 전달합니다.
public class ShopOrderExchangePaymentFailPO {
	// 결제번호입니다.
	private Long payNo;
	// 클레임번호입니다.
	private String clmNo;
	// Toss 실패 코드입니다.
	private String code;
	// Toss 실패 메시지입니다.
	private String message;
}
