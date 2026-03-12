package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 상품상세 예정 포인트 정보를 전달합니다.
public class ShopGoodsPointSummaryVO {
	// 계산 기준 고객등급 코드입니다.
	private String custGradeCd;
	// 포인트 적립률(%)입니다.
	private Integer pointSaveRate;
	// 예정 적립 포인트입니다.
	private Integer expectedPoint;
}
