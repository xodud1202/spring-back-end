package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문상세 페이지 응답을 전달합니다.
public class ShopMypageOrderDetailPageVO {
	// 주문번호 단위 주문 정보입니다.
	private ShopMypageOrderGroupVO order;
	// 주문 금액 요약 정보입니다.
	private ShopMypageOrderAmountSummaryVO amountSummary;
}
