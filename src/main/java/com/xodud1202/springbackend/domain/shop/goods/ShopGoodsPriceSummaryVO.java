package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 상품 가격 표시 요약 정보를 전달합니다.
public class ShopGoodsPriceSummaryVO {
	// 공급가입니다.
	private Integer supplyAmt;
	// 판매가입니다.
	private Integer saleAmt;
	// 공급가 취소선 노출 여부입니다.
	private boolean showSupplyStrike;
	// 할인율(정수 %)입니다.
	private Integer discountRate;
}
