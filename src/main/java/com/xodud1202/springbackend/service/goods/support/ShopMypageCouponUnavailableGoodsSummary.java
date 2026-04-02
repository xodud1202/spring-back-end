package com.xodud1202.springbackend.service.goods.support;

import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponUnavailableGoodsVO;

import java.util.List;

// 쿠폰 사용 불가 상품 요약 정보를 전달합니다.
public class ShopMypageCouponUnavailableGoodsSummary {
	// 쿠폰 사용 불가 상품 전체 건수입니다.
	private final int unavailableGoodsCount;
	// 쿠폰 사용 불가 상품 목록입니다.
	private final List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList;

	// 쿠폰 사용 불가 상품 요약 정보를 생성합니다.
	public ShopMypageCouponUnavailableGoodsSummary(
		int unavailableGoodsCount,
		List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList
	) {
		this.unavailableGoodsCount = Math.max(unavailableGoodsCount, 0);
		this.unavailableGoodsList = unavailableGoodsList == null ? List.of() : unavailableGoodsList;
	}

	// 빈 요약 정보를 반환합니다.
	public static ShopMypageCouponUnavailableGoodsSummary empty() {
		return new ShopMypageCouponUnavailableGoodsSummary(0, List.of());
	}

	// 쿠폰 사용 불가 상품 전체 건수를 반환합니다.
	public int getUnavailableGoodsCount() {
		return unavailableGoodsCount;
	}

	// 쿠폰 사용 불가 상품 목록을 반환합니다.
	public List<ShopMypageCouponUnavailableGoodsVO> getUnavailableGoodsList() {
		return unavailableGoodsList;
	}
}

