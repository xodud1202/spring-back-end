package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 배송 준비중 변경 요청을 정의합니다.
public class AdminOrderStartDeliveryPreparePO {
	// 처리 대상 상품 목록입니다.
	private List<AdminOrderStartDeliveryPrepareItemPO> itemList;
}
