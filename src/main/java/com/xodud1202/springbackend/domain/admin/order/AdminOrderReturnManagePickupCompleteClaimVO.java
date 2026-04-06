package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수완료 검수 팝업의 클레임 기본 정보를 전달합니다.
public class AdminOrderReturnManagePickupCompleteClaimVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 클레임 신청 일시입니다.
	private String chgDt;
}
