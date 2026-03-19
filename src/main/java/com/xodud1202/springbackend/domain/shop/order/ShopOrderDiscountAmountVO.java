package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문서 할인 금액 요약 정보를 전달합니다.
public class ShopOrderDiscountAmountVO {
	// 상품쿠폰 할인 금액입니다.
	private Integer goodsCouponDiscountAmt;
	// 장바구니 쿠폰 할인 금액입니다.
	private Integer cartCouponDiscountAmt;
	// 배송비 쿠폰 할인 금액입니다.
	private Integer deliveryCouponDiscountAmt;
	// 전체 쿠폰 할인 금액입니다.
	private Integer couponDiscountAmt;
	// 최대 포인트 사용 가능 금액입니다.
	private Integer maxPointUseAmt;
}
