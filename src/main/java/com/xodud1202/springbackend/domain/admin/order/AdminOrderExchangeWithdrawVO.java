package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문교환 철회 결과를 전달합니다.
public class AdminOrderExchangeWithdrawVO {
	// 주문번호입니다.
	private String ordNo;
	// 철회 처리로 변경된 상세/클레임 건수입니다.
	private Integer updatedCount;
	// 철회로 종료된 클레임 건수입니다.
	private Integer closedClaimCount;
	// 철회와 함께 결제 취소 또는 환불이 처리된 건수입니다.
	private Integer paymentCancelCount;
}
