package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문 결제 준비 응답 데이터를 전달합니다.
public class ShopOrderPaymentPrepareVO {
	// 생성된 주문번호입니다.
	private String ordNo;
	// 생성된 결제번호입니다.
	private Long payNo;
	// Toss 클라이언트 키입니다.
	private String clientKey;
	// Toss 결제수단 코드입니다.
	private String method;
	// Toss orderId 값입니다.
	private String orderId;
	// Toss 주문명입니다.
	private String orderName;
	// 결제 요청 금액입니다.
	private Long amount;
	// Toss 고객 식별키입니다.
	private String customerKey;
	// 주문 고객명입니다.
	private String customerName;
	// 주문 고객 이메일입니다.
	private String customerEmail;
	// 주문 고객 연락처입니다.
	private String customerMobilePhone;
	// 결제 성공 URL입니다.
	private String successUrl;
	// 결제 실패 URL입니다.
	private String failUrl;
}
