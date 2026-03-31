package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 배송 시작 관리 목록 검색 조건을 정의합니다.
public class AdminOrderStartDeliveryPO {
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 시작 오프셋입니다.
	private Integer offset;
	// 주문상세 상태 코드입니다.
	private String ordDtlStatCd;
}
