package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 관리 조회 조건을 전달합니다.
public class AdminOrderReturnManagePO {
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 오프셋입니다.
	private Integer offset;
	// 반품 상세 상태 코드입니다.
	private String chgDtlStatCd;
}
