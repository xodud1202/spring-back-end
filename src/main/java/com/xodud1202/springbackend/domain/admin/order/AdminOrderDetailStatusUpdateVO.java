package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문상세 상태 변경 응답을 정의합니다.
public class AdminOrderDetailStatusUpdateVO {
	// 변경된 주문상세 건수입니다.
	private Integer updatedCount;
}
