package com.xodud1202.springbackend.domain.shop.mypage;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 전달합니다.
public class ShopMypageOrderReturnPageVO {
	// 주문 그룹 정보입니다.
	private ShopMypageOrderGroupVO order;
	// 현재 주문 금액 요약 정보입니다.
	private ShopMypageOrderAmountSummaryVO amountSummary;
	// 반품 사유 코드 목록입니다.
	private List<ShopMypageOrderCancelReasonVO> reasonList;
	// 현재 사이트 배송 기준 정보입니다.
	private ShopCartSiteInfoVO siteInfo;
}
