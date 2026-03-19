package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 주문서 쿠폰 선택 후보 목록을 전달합니다.
public class ShopOrderCouponOptionVO {
	// 주문 상품 행별 상품쿠폰 후보 목록입니다.
	private List<ShopOrderGoodsCouponGroupVO> goodsCouponGroupList;
	// 장바구니 쿠폰 후보 목록입니다.
	private List<ShopOrderCouponItemVO> cartCouponList;
	// 배송비 쿠폰 후보 목록입니다.
	private List<ShopOrderCouponItemVO> deliveryCouponList;
}
