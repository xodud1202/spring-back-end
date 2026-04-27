package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 요청 상품 1건을 전달합니다.
public class ShopOrderExchangeItemPO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 교환 요청 수량입니다.
	private Integer exchangeQty;
	// 교환 희망 사이즈입니다.
	private String targetSizeId;
	// 교환 사유 코드입니다.
	private String reasonCd;
	// 교환 사유 상세입니다.
	private String reasonDetail;
}
