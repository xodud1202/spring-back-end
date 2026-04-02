package com.xodud1202.springbackend.service.order.support;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderGoodsCouponSelectionVO;

import java.util.List;

/**
 * @param selectionList 상품행별 선택 목록입니다.
 * @param discountAmt   상품쿠폰 할인 합계입니다.
 */ // 주문서 상품쿠폰 자동 선택 결과를 전달합니다.
public record ShopOrderGoodsCouponMatchResult(List<ShopOrderGoodsCouponSelectionVO> selectionList, int discountAmt) {
	// 주문서 상품쿠폰 자동 선택 결과를 생성합니다.
	public ShopOrderGoodsCouponMatchResult {
		selectionList = selectionList == null ? List.of() : selectionList;
		discountAmt = Math.max(discountAmt, 0);
	}

	// 상품행별 선택 목록을 반환합니다.
	public List<ShopOrderGoodsCouponSelectionVO> getSelectionList() {
		return selectionList;
	}

	// 상품쿠폰 할인 합계를 반환합니다.
	public int getDiscountAmt() {
		return discountAmt;
	}
}
