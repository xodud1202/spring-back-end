package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수완료 저장 결과를 전달합니다.
public class AdminOrderReturnManagePickupCompleteResultVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 환불 결제번호입니다.
	private Long refundPayNo;
	// 환불된 현금 금액입니다.
	private Long refundedCashAmt;
	// 복구된 포인트 금액입니다.
	private Long restoredPointAmt;
	// 재지급된 포인트 금액입니다.
	private Long reissuedPointAmt;
}
