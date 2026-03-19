package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 주문서 할인 재계산 요청 정보를 전달합니다.
public class ShopOrderDiscountQuotePO {
	// 주문 대상 장바구니 번호 목록입니다.
	private List<Long> cartIdList;
	// 주문 상품 행별 상품쿠폰 선택 목록입니다.
	private List<ShopOrderGoodsCouponSelectionVO> goodsCouponSelectionList;
	// 선택한 장바구니 쿠폰 고객 보유 번호입니다.
	private Long cartCouponCustCpnNo;
	// 선택한 배송비 쿠폰 고객 보유 번호입니다.
	private Long deliveryCouponCustCpnNo;
}
