package com.xodud1202.springbackend.service.order.support;

// 주문취소 화면 내부 계산용 금액 요약을 전달합니다.
public class ShopOrderCancelPreviewSummary {
	// 공급가 합계입니다.
	private long totalSupplyAmt;
	// 상품 판매가 합계입니다.
	private long totalOrderAmt;
	// 상품쿠폰 환급 합계입니다.
	private long totalGoodsCouponDiscountAmt;
	// 장바구니쿠폰 환급 합계입니다.
	private long totalCartCouponDiscountAmt;
	// 포인트 환급 합계입니다.
	private long totalPointRefundAmt;

	// 주문취소 화면 내부 계산용 금액 요약 객체를 생성합니다.
	public ShopOrderCancelPreviewSummary() {
	}

	// 공급가 합계를 반환합니다.
	public long getTotalSupplyAmt() {
		return totalSupplyAmt;
	}

	// 공급가 합계를 저장합니다.
	public void setTotalSupplyAmt(long totalSupplyAmt) {
		this.totalSupplyAmt = totalSupplyAmt;
	}

	// 상품 판매가 합계를 반환합니다.
	public long getTotalOrderAmt() {
		return totalOrderAmt;
	}

	// 상품 판매가 합계를 저장합니다.
	public void setTotalOrderAmt(long totalOrderAmt) {
		this.totalOrderAmt = totalOrderAmt;
	}

	// 상품쿠폰 환급 합계를 반환합니다.
	public long getTotalGoodsCouponDiscountAmt() {
		return totalGoodsCouponDiscountAmt;
	}

	// 상품쿠폰 환급 합계를 저장합니다.
	public void setTotalGoodsCouponDiscountAmt(long totalGoodsCouponDiscountAmt) {
		this.totalGoodsCouponDiscountAmt = totalGoodsCouponDiscountAmt;
	}

	// 장바구니쿠폰 환급 합계를 반환합니다.
	public long getTotalCartCouponDiscountAmt() {
		return totalCartCouponDiscountAmt;
	}

	// 장바구니쿠폰 환급 합계를 저장합니다.
	public void setTotalCartCouponDiscountAmt(long totalCartCouponDiscountAmt) {
		this.totalCartCouponDiscountAmt = totalCartCouponDiscountAmt;
	}

	// 포인트 환급 합계를 반환합니다.
	public long getTotalPointRefundAmt() {
		return totalPointRefundAmt;
	}

	// 포인트 환급 합계를 저장합니다.
	public void setTotalPointRefundAmt(long totalPointRefundAmt) {
		this.totalPointRefundAmt = totalPointRefundAmt;
	}
}
