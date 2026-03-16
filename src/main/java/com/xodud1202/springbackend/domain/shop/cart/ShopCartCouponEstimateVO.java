package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 장바구니 쿠폰 예상 할인 계산 결과를 전달합니다.
public class ShopCartCouponEstimateVO {
	// 예상 최대 쿠폰 할인 금액입니다.
	private Integer expectedMaxDiscountAmt;
	// 상품쿠폰 예상 할인 금액입니다.
	private Integer goodsCouponDiscountAmt;
	// 장바구니쿠폰 예상 할인 금액입니다.
	private Integer cartCouponDiscountAmt;
	// 배송비쿠폰 예상 할인 금액입니다.
	private Integer deliveryCouponDiscountAmt;
}
