package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품상세 환불 예정 금액 요약을 전달합니다.
public class ShopMypageReturnPreviewAmountVO {
	// 상품가격 합계입니다.
	private Long totalSupplyAmt;
	// 상품할인 합계입니다.
	private Long totalGoodsDiscountAmt;
	// 상품쿠폰 환급 금액입니다.
	private Long totalGoodsCouponDiscountAmt;
	// 장바구니쿠폰 환급 금액입니다.
	private Long totalCartCouponDiscountAmt;
	// 배송비쿠폰 환급 금액입니다.
	private Long deliveryCouponRefundAmt;
	// 포인트 환급 금액입니다.
	private Long totalPointRefundAmt;
	// 실결제 상품가입니다.
	private Long paidGoodsAmt;
	// 환급 혜택 합계입니다.
	private Long benefitAmt;
	// 배송비 환급/차감 조정 금액입니다.
	private Long shippingAdjustmentAmt;
	// 반품 예정 금액입니다.
	private Long expectedRefundAmt;
}
