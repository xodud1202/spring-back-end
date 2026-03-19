package com.xodud1202.springbackend.domain.shop.order;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 주문서 페이지 응답 데이터를 전달합니다.
public class ShopOrderPageVO {
	// 주문 대상 상품 목록입니다.
	private List<ShopCartItemVO> cartList;
	// 주문 대상 상품 건수입니다.
	private Integer cartCount;
	// 배송비 기준 사이트 정보입니다.
	private ShopCartSiteInfoVO siteInfo;
	// 등록된 배송지 목록입니다.
	private List<ShopOrderAddressVO> addressList;
	// 현재 기본 배송지입니다.
	private ShopOrderAddressVO defaultAddress;
	// 현재 사용 가능한 보유 포인트입니다.
	private Integer availablePointAmt;
	// 주문서 쿠폰 선택 후보 목록입니다.
	private ShopOrderCouponOptionVO couponOption;
	// 주문서 초기 자동 선택 쿠폰 정보입니다.
	private ShopOrderDiscountSelectionVO discountSelection;
	// 주문서 초기 할인 금액 요약 정보입니다.
	private ShopOrderDiscountAmountVO discountAmount;
}
