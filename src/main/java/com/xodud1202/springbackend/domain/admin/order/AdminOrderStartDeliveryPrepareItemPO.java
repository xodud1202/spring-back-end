package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 배송 준비중 처리 대상 상품 정보를 정의합니다.
public class AdminOrderStartDeliveryPrepareItemPO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 배송업체 코드입니다.
	private String delvCompCd;
	// 송장번호입니다.
	private String invoiceNo;
}
