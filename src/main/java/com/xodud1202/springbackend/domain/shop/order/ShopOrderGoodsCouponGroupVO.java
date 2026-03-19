package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 주문 상품 행별 상품쿠폰 선택 후보 정보를 전달합니다.
public class ShopOrderGoodsCouponGroupVO {
	// 장바구니 번호입니다.
	private Long cartId;
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 사이즈 코드입니다.
	private String sizeId;
	// 적용 가능한 상품쿠폰 목록입니다.
	private List<ShopOrderCouponItemVO> couponList;
}
