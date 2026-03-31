package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 배송 시작 관리 상태 변경 대상 키 정보를 정의합니다.
public class AdminOrderStartDeliveryKeyItemPO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
}
