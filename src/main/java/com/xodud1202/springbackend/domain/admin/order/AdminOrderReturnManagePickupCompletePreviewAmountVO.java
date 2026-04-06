package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수완료 검수 팝업의 고정 금액 요약 정보를 전달합니다.
public class AdminOrderReturnManagePickupCompletePreviewAmountVO {
	// 총 상품가격입니다.
	private Long totalSupplyAmt;
	// 총 상품할인 금액입니다.
	private Long totalGoodsDiscountAmt;
	// 총 상품쿠폰 차감 금액입니다.
	private Long totalGoodsCouponDiscountAmt;
	// 총 장바구니쿠폰 차감 금액입니다.
	private Long totalCartCouponDiscountAmt;
	// 배송비쿠폰 환급 금액입니다.
	private Long deliveryCouponRefundAmt;
	// 총 포인트 환급 금액입니다.
	private Long totalPointRefundAmt;
	// 실결제 상품가입니다.
	private Long paidGoodsAmt;
	// 환급 혜택 합계입니다.
	private Long benefitAmt;
}
