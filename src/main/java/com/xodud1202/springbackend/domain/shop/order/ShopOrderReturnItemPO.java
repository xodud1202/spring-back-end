package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문반품 요청의 개별 주문상품 정보를 전달합니다.
public class ShopOrderReturnItemPO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 반품 요청 수량입니다.
	private Integer returnQty;
	// 개별 상품 반품 사유 코드입니다.
	private String reasonCd;
	// 개별 상품 반품 사유 상세 내용입니다.
	private String reasonDetail;
}
