package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 반품 회수 신청 저장 요청을 전달합니다.
public class AdminOrderReturnManagePickupRequestPO {
	// 처리 대상 클레임 목록입니다.
	private List<AdminOrderReturnManagePickupItemPO> itemList;
}
