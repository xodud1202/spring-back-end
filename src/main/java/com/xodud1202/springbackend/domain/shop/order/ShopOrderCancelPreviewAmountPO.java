package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문취소 화면에서 계산한 취소 예정 금액 요약을 전달합니다.
public class ShopOrderCancelPreviewAmountPO {
	// 취소 예정 총액입니다.
	private Long expectedRefundAmt;
	// 실결제 상품 환불 금액입니다.
	private Long paidGoodsAmt;
	// 환급 혜택 합계입니다.
	private Long benefitAmt;
	// 배송비 조정 금액입니다.
	private Long shippingAdjustmentAmt;
	// 포인트 환급 금액입니다.
	private Long totalPointRefundAmt;
	// 배송비 쿠폰 환급 금액입니다.
	private Long deliveryCouponRefundAmt;
}
