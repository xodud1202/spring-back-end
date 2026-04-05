package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 신청 저장 대상 1건을 전달합니다.
public class AdminOrderReturnManagePickupItemPO {
	// 클레임번호입니다.
	private String clmNo;
	// 회수 택배사 코드입니다.
	private String delvCompCd;
	// 회수 송장번호입니다.
	private String invoiceNo;
}
