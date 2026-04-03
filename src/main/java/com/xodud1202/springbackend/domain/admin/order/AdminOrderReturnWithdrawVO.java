package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문반품 철회 결과를 전달합니다.
public class AdminOrderReturnWithdrawVO {
	// 주문번호입니다.
	private String ordNo;
	// 철회된 상세 건수입니다.
	private Integer updatedCount;
	// 철회로 종료된 클레임 건수입니다.
	private Integer closedClaimCount;
}
