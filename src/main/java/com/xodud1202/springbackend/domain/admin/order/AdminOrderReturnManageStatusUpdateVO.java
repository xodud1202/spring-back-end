package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 관리 상태 변경 응답을 전달합니다.
public class AdminOrderReturnManageStatusUpdateVO {
	// 변경된 클레임 건수입니다.
	private Integer updatedCount;
}
