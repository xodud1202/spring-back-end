package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문서 할인 재계산 결과를 전달합니다.
public class ShopOrderDiscountQuoteVO {
	// 정규화된 쿠폰 선택 상태입니다.
	private ShopOrderDiscountSelectionVO discountSelection;
	// 재계산된 할인 금액 요약입니다.
	private ShopOrderDiscountAmountVO discountAmount;
}
