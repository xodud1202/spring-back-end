package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 장바구니 쿠폰 예상 할인 계산 대상 행 정보를 전달합니다.
public class ShopCartCouponEstimateItemPO {
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
}
