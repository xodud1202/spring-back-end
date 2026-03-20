package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문내역 상태 요약 건수를 전달합니다.
public class ShopMypageOrderStatusSummaryVO {
	// 무통장 입금대기 주문상세 건수입니다.
	private Integer waitingForDepositCount;
	// 결제완료 주문상세 건수입니다.
	private Integer paymentCompletedCount;
	// 상품준비중 주문상세 건수입니다.
	private Integer productPreparingCount;
	// 배송준비중 주문상세 건수입니다.
	private Integer deliveryPreparingCount;
	// 배송중 주문상세 건수입니다.
	private Integer shippingCount;
	// 배송완료 주문상세 건수입니다.
	private Integer deliveryCompletedCount;
	// 구매확정 주문상세 건수입니다.
	private Integer purchaseConfirmedCount;
}
