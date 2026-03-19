package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문 결제 실패 반영 요청 데이터를 전달합니다.
public class ShopOrderPaymentFailPO {
	// 결제번호입니다.
	private Long payNo;
	// 주문번호입니다.
	private String ordNo;
	// Toss 실패 코드입니다.
	private String code;
	// Toss 실패 메시지입니다.
	private String message;
}
