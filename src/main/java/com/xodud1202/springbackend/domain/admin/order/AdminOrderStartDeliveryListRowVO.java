package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 배송 시작 관리 목록 행 정보를 정의합니다.
public class AdminOrderStartDeliveryListRowVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 상품명입니다.
	private String goodsNm;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 배송업체 코드입니다.
	private String delvCompCd;
	// 배송업체명입니다.
	private String delvCompNm;
	// 송장번호입니다.
	private String invoiceNo;
	// 결제일시입니다.
	private String payDt;
	// 주문상세 상태 코드입니다.
	private String ordDtlStatCd;
	// 주문상세 상태명입니다.
	private String ordDtlStatNm;
}
