package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 관리 클레임 검증 요약 1건을 전달합니다.
public class AdminOrderReturnManageClaimSummaryVO {
	// 클레임번호입니다.
	private String clmNo;
	// 클레임 상세 건수입니다.
	private Integer detailCount;
	// 클레임 내 최소 반품 상세 상태 코드입니다.
	private String minChgDtlStatCd;
	// 클레임 내 최대 반품 상세 상태 코드입니다.
	private String maxChgDtlStatCd;
	// 회수지 행 존재 건수입니다.
	private Integer pickupAddressCount;
}
