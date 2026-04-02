package com.xodud1202.springbackend.service.goods.support;

/**
 * @param categoryId 카테고리코드입니다.
 * @param goodsId    상품코드입니다.
 * @param dispOrd    정렬순서입니다.
 */ // 카테고리 상품 엑셀 업로드 행 정보를 전달합니다.
public record CategoryGoodsExcelRow(String categoryId, String goodsId, Integer dispOrd) {
	// 카테고리 상품 엑셀 업로드 행 정보를 생성합니다.
	public CategoryGoodsExcelRow {
	}
}
