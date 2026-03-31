package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 배송 상태 변경 결과를 정의합니다.
public class AdminOrderStartDeliveryStatusUpdateVO {
	// 변경된 건수입니다.
	private int updatedCount;
}
