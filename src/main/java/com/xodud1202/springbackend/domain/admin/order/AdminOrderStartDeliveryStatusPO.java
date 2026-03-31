package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 배송 상태 변경 요청을 정의합니다.
public class AdminOrderStartDeliveryStatusPO {
	// 처리 대상 상품 키 목록입니다.
	private List<AdminOrderStartDeliveryKeyItemPO> itemList;
}
