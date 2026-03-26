package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 주문 목록 응답 정보를 정의합니다.
public class AdminOrderListResponseVO {
	// 주문 목록입니다.
	private List<AdminOrderListRowVO> list;
	// 전체 건수입니다.
	private Integer totalCount;
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
}
