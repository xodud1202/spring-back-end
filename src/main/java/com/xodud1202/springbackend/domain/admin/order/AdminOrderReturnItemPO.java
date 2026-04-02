package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문반품 요청의 개별 주문상품 정보를 전달합니다.
public class AdminOrderReturnItemPO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 반품 요청 수량입니다.
	private Integer returnQty;
}
