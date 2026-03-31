package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 주문상세 상태 변경 요청을 정의합니다.
public class AdminOrderDetailStatusUpdatePO {
	// 주문번호입니다.
	private String ordNo;
	// 상태 변경 대상 주문상세번호 목록입니다.
	private List<Integer> ordDtlNoList;
}
