package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 무통장입금 복구용 장바구니 재생성 정보를 전달합니다.
public class ShopOrderRestoreCartItemVO {
	// 고객번호입니다.
	private Long custNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 주문수량입니다.
	private Integer ordQty;
	// 기획전 번호입니다.
	private Integer exhibitionNo;
}
