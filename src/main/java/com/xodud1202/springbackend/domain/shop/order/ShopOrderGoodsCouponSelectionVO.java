package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문 상품 행별 상품쿠폰 선택 정보를 전달합니다.
public class ShopOrderGoodsCouponSelectionVO {
	// 장바구니 번호입니다.
	private Long cartId;
	// 선택한 고객 보유 쿠폰 번호입니다.
	private Long custCpnNo;
}
