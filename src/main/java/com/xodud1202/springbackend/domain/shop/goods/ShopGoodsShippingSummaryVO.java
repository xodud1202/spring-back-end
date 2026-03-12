package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 상품상세 배송비 노출 요약 정보를 전달합니다.
public class ShopGoodsShippingSummaryVO {
	// 무료배송 여부입니다.
	private boolean freeDelivery;
	// 배송비입니다.
	private Integer deliveryFee;
	// 무료배송 기준 금액입니다.
	private Integer deliveryFeeLimit;
	// 화면 노출용 배송비 문구입니다.
	private String shippingMessage;
}
