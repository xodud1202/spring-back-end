package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문서 결제 환경 정보를 전달합니다.
public class ShopOrderPaymentConfigVO {
	// Toss 클라이언트 키입니다.
	private String clientKey;
	// Toss API 버전입니다.
	private String apiVersion;
	// 결제 성공 URL 베이스입니다.
	private String successUrlBase;
	// 결제 실패 URL 베이스입니다.
	private String failUrlBase;
}
