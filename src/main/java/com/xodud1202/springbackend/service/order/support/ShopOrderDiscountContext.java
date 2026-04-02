package com.xodud1202.springbackend.service.order.support;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartCustomerCouponVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponTargetVO;

import java.util.List;
import java.util.Map;

/**
 * @param cartItemList       주문 대상 장바구니 목록입니다.
 * @param estimateRowList    쿠폰 계산용 행 목록입니다.
 * @param customerCouponList 현재 사용 가능한 전체 보유 쿠폰 목록입니다.
 * @param goodsCouponList    상품쿠폰 목록입니다.
 * @param cartCouponList     장바구니 쿠폰 목록입니다.
 * @param deliveryCouponList 배송비 쿠폰 목록입니다.
 * @param couponTargetMap    쿠폰 번호별 적용 대상 목록입니다.
 * @param siteInfo           배송비 기준 사이트 정보입니다.
 * @param availablePointAmt  현재 사용 가능한 보유 포인트입니다.
 */ // 주문서 할인 계산에 필요한 컨텍스트를 전달합니다.
public record ShopOrderDiscountContext(
	List<ShopCartItemVO> cartItemList,
	List<ShopCartCouponEstimateRow> estimateRowList,
	List<ShopCartCustomerCouponVO> customerCouponList,
	List<ShopCartCustomerCouponVO> goodsCouponList,
	List<ShopCartCustomerCouponVO> cartCouponList,
	List<ShopCartCustomerCouponVO> deliveryCouponList,
	Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap,
	ShopCartSiteInfoVO siteInfo,
	int availablePointAmt
) {
	// 주문서 할인 계산 컨텍스트를 생성합니다.
	public ShopOrderDiscountContext {
		cartItemList = cartItemList == null ? List.of() : cartItemList;
		estimateRowList = estimateRowList == null ? List.of() : estimateRowList;
		customerCouponList = customerCouponList == null ? List.of() : customerCouponList;
		goodsCouponList = goodsCouponList == null ? List.of() : goodsCouponList;
		cartCouponList = cartCouponList == null ? List.of() : cartCouponList;
		deliveryCouponList = deliveryCouponList == null ? List.of() : deliveryCouponList;
		couponTargetMap = couponTargetMap == null ? Map.of() : couponTargetMap;
		availablePointAmt = Math.max(availablePointAmt, 0);
	}

	// 주문 대상 장바구니 목록을 반환합니다.
	public List<ShopCartItemVO> getCartItemList() {
		return cartItemList;
	}

	// 쿠폰 계산용 행 목록을 반환합니다.
	public List<ShopCartCouponEstimateRow> getEstimateRowList() {
		return estimateRowList;
	}

	// 현재 사용 가능한 전체 보유 쿠폰 목록을 반환합니다.
	public List<ShopCartCustomerCouponVO> getCustomerCouponList() {
		return customerCouponList;
	}

	// 상품쿠폰 목록을 반환합니다.
	public List<ShopCartCustomerCouponVO> getGoodsCouponList() {
		return goodsCouponList;
	}

	// 장바구니 쿠폰 목록을 반환합니다.
	public List<ShopCartCustomerCouponVO> getCartCouponList() {
		return cartCouponList;
	}

	// 배송비 쿠폰 목록을 반환합니다.
	public List<ShopCartCustomerCouponVO> getDeliveryCouponList() {
		return deliveryCouponList;
	}

	// 쿠폰 번호별 적용 대상 목록을 반환합니다.
	public Map<Long, List<ShopGoodsCouponTargetVO>> getCouponTargetMap() {
		return couponTargetMap;
	}

	// 배송비 기준 사이트 정보를 반환합니다.
	public ShopCartSiteInfoVO getSiteInfo() {
		return siteInfo;
	}

	// 현재 사용 가능한 보유 포인트를 반환합니다.
	public int getAvailablePointAmt() {
		return availablePointAmt;
	}
}
