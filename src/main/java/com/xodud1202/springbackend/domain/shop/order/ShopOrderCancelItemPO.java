package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문취소 요청의 개별 주문상품 정보를 전달합니다.
public class ShopOrderCancelItemPO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 취소 요청 수량입니다.
	private Integer cancelQty;
	// 개별 상품 취소 사유 코드입니다.
	private String reasonCd;
	// 개별 상품 취소 사유 상세 내용입니다.
	private String reasonDetail;
}
