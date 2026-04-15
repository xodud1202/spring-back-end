package com.xodud1202.springbackend.service.order.support;

import lombok.Getter;
import lombok.Setter;

// 주문취소 화면 내부 계산용 금액 요약을 전달합니다.
@Setter
@Getter
public class ShopOrderCancelPreviewSummary {
	// 공급가 합계를 저장합니다.
	// 공급가 합계를 반환합니다.
	// 공급가 합계입니다.
	private long totalSupplyAmt;
	// 상품 판매가 합계를 저장합니다.
	// 상품 판매가 합계를 반환합니다.
	// 상품 판매가 합계입니다.
	private long totalOrderAmt;
	// 상품쿠폰 환급 합계를 저장합니다.
	// 상품쿠폰 환급 합계를 반환합니다.
	// 상품쿠폰 환급 합계입니다.
	private long totalGoodsCouponDiscountAmt;
	// 장바구니쿠폰 환급 합계를 저장합니다.
	// 장바구니쿠폰 환급 합계를 반환합니다.
	// 장바구니쿠폰 환급 합계입니다.
	private long totalCartCouponDiscountAmt;
	// 포인트 환급 합계를 저장합니다.
	// 포인트 환급 합계를 반환합니다.
	// 포인트 환급 합계입니다.
	private long totalPointRefundAmt;

	// 주문취소 화면 내부 계산용 금액 요약 객체를 생성합니다.
	public ShopOrderCancelPreviewSummary() {
	}

}
