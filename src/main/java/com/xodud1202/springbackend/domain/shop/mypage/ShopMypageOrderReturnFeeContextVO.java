package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품 배송비 계산 컨텍스트를 전달합니다.
public class ShopMypageOrderReturnFeeContextVO {
	// 원주문 실결제 배송비입니다.
	private Integer originalPaidDeliveryAmt;
	// 원주문 무료배송 여부입니다.
	private Boolean originalFreeDeliveryYn;
	// 같은 주문에 회사 귀책 반품/교환 이력이 있는지 여부입니다.
	private Boolean hasPriorCompanyFaultReturnOrExchange;
	// 같은 주문에 고객 귀책 배송비 차감 반품 이력이 있는지 여부입니다.
	private Boolean hasPriorCustomerFaultReturnDeduction;
	// 현재 주문의 잔여 결제금액입니다.
	private Integer currentRemainingFinalPayAmt;
}
