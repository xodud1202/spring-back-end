package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문취소 화면에서 계산한 취소 예정 금액 요약을 전달합니다.
public class ShopOrderCancelPreviewAmountPO {
	// 실환불 기준 취소 예정 금액입니다.
	private Long expectedRefundAmt;
	// 상품 판매가 기준 실결제 상품가입니다.
	private Long paidGoodsAmt;
	// 상품쿠폰/장바구니쿠폰/포인트 환급 합계입니다.
	private Long benefitAmt;
	// 환급 배송비 또는 차감 배송비입니다.
	private Long shippingAdjustmentAmt;
	// 포인트 환급 금액입니다.
	private Long totalPointRefundAmt;
	// 배송비 쿠폰 환급 금액입니다.
	private Long deliveryCouponRefundAmt;
}
