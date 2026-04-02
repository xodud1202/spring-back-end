package com.xodud1202.springbackend.service.order.support;

import java.util.Set;

/**
 * @param cartId             장바구니 번호입니다.
 * @param goodsId            상품코드입니다.
 * @param sizeId             사이즈코드입니다.
 * @param rowSaleAmt         행 단위 판매가 합계입니다.
 * @param brandNoValue       브랜드 번호 문자열입니다.
 * @param categoryIdSet      상품 카테고리 코드 목록입니다.
 * @param exhibitionTabNoSet 상품 기획전 탭 번호 목록입니다.
 */ // 장바구니 쿠폰 예상 할인 계산용 상품 행 컨텍스트를 전달합니다.
public record ShopCartCouponEstimateRow(
	Long cartId,
	String goodsId,
	String sizeId,
	int rowSaleAmt,
	String brandNoValue,
	Set<String> categoryIdSet,
	Set<String> exhibitionTabNoSet
) {
	// 장바구니 쿠폰 예상 할인 계산용 상품 행 컨텍스트를 생성합니다.
	public ShopCartCouponEstimateRow {
		categoryIdSet = categoryIdSet == null ? Set.of() : categoryIdSet;
		exhibitionTabNoSet = exhibitionTabNoSet == null ? Set.of() : exhibitionTabNoSet;
	}

	// 장바구니 번호를 반환합니다.
	public Long getCartId() {
		return cartId;
	}

	// 상품코드를 반환합니다.
	public String getGoodsId() {
		return goodsId;
	}

	// 사이즈코드를 반환합니다.
	public String getSizeId() {
		return sizeId;
	}

	// 행 단위 판매가 합계를 반환합니다.
	public int getRowSaleAmt() {
		return rowSaleAmt;
	}

	// 브랜드 번호 문자열을 반환합니다.
	public String getBrandNoValue() {
		return brandNoValue;
	}

	// 상품 카테고리 코드 목록을 반환합니다.
	public Set<String> getCategoryIdSet() {
		return categoryIdSet;
	}

	// 상품 기획전 탭 번호 목록을 반환합니다.
	public Set<String> getExhibitionTabNoSet() {
		return exhibitionTabNoSet;
	}
}
