package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문취소 완료 결과를 전달합니다.
public class ShopOrderCancelResultVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 환불 결제번호입니다.
	private Long refundPayNo;
	// 환불 결제 상태코드입니다.
	private String payStatCd;
	// PG 현금 환불 금액입니다.
	private Long refundedCashAmt;
	// 복구된 포인트 금액입니다.
	private Long restoredPointAmt;
}
