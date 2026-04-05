package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 관리 클레임 키 1건을 전달합니다.
public class AdminOrderReturnManageClaimItemPO {
	// 클레임번호입니다.
	private String clmNo;
}
