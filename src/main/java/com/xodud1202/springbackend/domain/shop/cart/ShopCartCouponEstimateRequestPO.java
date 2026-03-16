package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

import java.util.List;

@Data
// 장바구니 쿠폰 예상 할인 계산 요청 정보를 전달합니다.
public class ShopCartCouponEstimateRequestPO {
	// 선택한 장바구니 행 목록입니다.
	private List<ShopCartCouponEstimateItemPO> cartItemList;
}
