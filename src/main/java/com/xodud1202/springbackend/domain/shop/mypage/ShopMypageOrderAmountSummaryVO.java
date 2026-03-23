package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문상세 금액 요약 정보를 전달합니다.
public class ShopMypageOrderAmountSummaryVO {
	// 총 공급가입니다.
	private Long totalSupplyAmt;
	// 총 상품금액입니다.
	private Long totalOrderAmt;
	// 총 상품할인 금액입니다.
	private Long totalGoodsDiscountAmt;
	// 총 상품쿠폰 할인 금액입니다.
	private Long totalGoodsCouponDiscountAmt;
	// 총 장바구니쿠폰 할인 금액입니다.
	private Long totalCartCouponDiscountAmt;
	// 총 쿠폰할인 금액입니다.
	private Long totalCouponDiscountAmt;
	// 총 포인트 사용 금액입니다.
	private Long totalPointUseAmt;
	// 배송비 금액입니다.
	private Long deliveryFeeAmt;
	// 배송비 쿠폰 할인 금액입니다.
	private Long deliveryCouponDiscountAmt;
	// 최종 결제 금액입니다.
	private Long finalPayAmt;
}
