package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문교환 철회 대상 상품 1건을 전달합니다.
public class AdminOrderExchangeWithdrawItemPO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
}
