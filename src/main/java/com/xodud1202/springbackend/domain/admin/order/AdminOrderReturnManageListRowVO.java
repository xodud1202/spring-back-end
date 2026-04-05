package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수 관리 목록 1건을 전달합니다.
public class AdminOrderReturnManageListRowVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 수량입니다.
	private Integer qty;
	// 상품명입니다.
	private String goodsNm;
	// 회수 택배사 코드입니다.
	private String delvCompCd;
	// 회수 택배사명입니다.
	private String delvCompNm;
	// 회수 송장번호입니다.
	private String invoiceNo;
	// 클레임 신청 일시입니다.
	private String chgDt;
	// 반품 상세 상태 코드입니다.
	private String chgDtlStatCd;
	// 반품 상세 상태명입니다.
	private String chgDtlStatNm;
}
