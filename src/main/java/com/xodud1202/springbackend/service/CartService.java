package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateRequestPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartDeletePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartOptionUpdatePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 장바구니 도메인 비즈니스 로직을 제공합니다.
public class CartService {
	private final GoodsService goodsService;

	// 쇼핑몰 상품 장바구니를 등록(기존 건은 수량 가산)하고 최종 수량을 반환합니다.
	public int addShopGoodsCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		return goodsService.addShopGoodsCart(goodsId, sizeId, qty, custNo, exhibitionNo);
	}

	// 쇼핑몰 바로구매용 장바구니를 신규 등록하고 생성된 장바구니 번호를 반환합니다.
	public Long addShopGoodsOrderNowCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		return goodsService.addShopGoodsOrderNowCart(goodsId, sizeId, qty, custNo, exhibitionNo);
	}

	// 쇼핑몰 장바구니 페이지 데이터를 조회합니다.
	public ShopCartPageVO getShopCartPage(Long custNo) {
		return goodsService.getShopCartPage(custNo);
	}

	// 쇼핑몰 장바구니 선택 상품 기준 예상 최대 쿠폰 할인 금액을 계산합니다.
	public ShopCartCouponEstimateVO getShopCartCouponEstimate(ShopCartCouponEstimateRequestPO param, Long custNo) {
		return goodsService.getShopCartCouponEstimate(param, custNo);
	}

	// 쇼핑몰 장바구니 상품 옵션(사이즈/수량)을 변경합니다.
	public void updateShopCartOption(ShopCartOptionUpdatePO param, Long custNo) {
		goodsService.updateShopCartOption(param, custNo);
	}

	// 쇼핑몰 장바구니 선택 상품을 삭제합니다.
	public int deleteShopCartItems(ShopCartDeletePO param, Long custNo) {
		return goodsService.deleteShopCartItems(param, custNo);
	}

	// 쇼핑몰 장바구니 전체 상품을 삭제합니다.
	public int deleteShopCartAll(Long custNo) {
		return goodsService.deleteShopCartAll(custNo);
	}
}
