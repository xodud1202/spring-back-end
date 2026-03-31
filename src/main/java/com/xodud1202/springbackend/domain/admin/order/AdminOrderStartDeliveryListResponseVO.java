package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 배송 시작 관리 목록 응답을 정의합니다.
public class AdminOrderStartDeliveryListResponseVO {
	// 목록입니다.
	private List<AdminOrderStartDeliveryListRowVO> list;
	// 전체 건수입니다.
	private int totalCount;
	// 현재 페이지입니다.
	private int page;
	// 페이지 크기입니다.
	private int pageSize;
}
