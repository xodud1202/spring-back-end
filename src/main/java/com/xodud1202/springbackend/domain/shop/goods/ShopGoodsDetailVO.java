package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 상품상세 상단 화면 응답 데이터를 전달합니다.
public class ShopGoodsDetailVO {
	// 기본 상품 정보입니다.
	private ShopGoodsBasicVO goods;
	// 상품 이미지 목록입니다.
	private List<ShopGoodsImageVO> images;
	// 동일 그룹 상품 목록(컬러 옵션)입니다.
	private List<ShopGoodsGroupItemVO> groupGoods;
	// 사이즈/재고 목록입니다.
	private List<ShopGoodsSizeItemVO> sizes;
	// 기기별 상품 상세설명입니다.
	private ShopGoodsDescVO detailDesc;
	// 위시리스트 상태입니다.
	private ShopGoodsWishlistVO wishlist;
	// 사이트 배송 기준 정보입니다.
	private ShopGoodsSiteInfoVO siteInfo;
	// 사용 가능한 상품쿠폰 목록입니다.
	private List<ShopGoodsCouponVO> coupons;
	// 가격 노출 요약 정보입니다.
	private ShopGoodsPriceSummaryVO priceSummary;
	// 예정 포인트 요약 정보입니다.
	private ShopGoodsPointSummaryVO pointSummary;
	// 배송비 노출 요약 정보입니다.
	private ShopGoodsShippingSummaryVO shippingSummary;
}
