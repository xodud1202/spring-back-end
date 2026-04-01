package com.xodud1202.springbackend.domain.admin.order;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderAmountSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnFeeContextVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import lombok.Data;

import java.util.List;

@Data
// 관리자 반품 신청 화면 데이터를 전달합니다.
public class AdminOrderReturnPageVO {
	// 주문 그룹 정보입니다.
	private ShopMypageOrderGroupVO order;
	// 현재 주문 금액 요약 정보입니다.
	private ShopMypageOrderAmountSummaryVO amountSummary;
	// 반품 사유 코드 목록입니다.
	private List<ShopMypageOrderCancelReasonVO> reasonList;
	// 현재 사이트 배송 기준 정보입니다.
	private ShopCartSiteInfoVO siteInfo;
	// 반품 배송비 계산 컨텍스트입니다.
	private ShopMypageOrderReturnFeeContextVO returnFeeContext;
	// 기본 반품 회수지 정보입니다.
	private ShopOrderAddressVO pickupAddress;
}
