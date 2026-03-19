package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문서 적립 예정 포인트 요약 정보를 전달합니다.
public class ShopOrderPointSaveSummaryVO {
	// 총 적립 예정 포인트입니다.
	private Integer totalExpectedPoint;
	// 포인트 적립률입니다.
	private Integer pointSaveRate;
}
