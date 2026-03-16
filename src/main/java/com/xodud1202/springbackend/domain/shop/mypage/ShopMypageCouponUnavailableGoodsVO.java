package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 쿠폰 사용 불가 상품 정보를 전달합니다.
public class ShopMypageCouponUnavailableGoodsVO {
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
}
