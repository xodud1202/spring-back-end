package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 주문반품 철회 요청 본문을 전달합니다.
public class AdminOrderReturnWithdrawPO {
	// 주문번호입니다.
	private String ordNo;
	// 철회 대상 클레임 상품 목록입니다.
	private List<AdminOrderReturnWithdrawItemPO> claimItemList;
}
