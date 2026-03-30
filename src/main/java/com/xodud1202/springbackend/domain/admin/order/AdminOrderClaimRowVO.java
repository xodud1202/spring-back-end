package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 상세 하단 클레임 행 정보를 정의합니다.
public class AdminOrderClaimRowVO {
	// 클레임 번호입니다.
	private String clmNo;
	// 클레임 상세 구분 코드입니다.
	private String chgDtlGbCd;
	// 클레임 상세 구분명입니다.
	private String chgDtlGbNm;
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 클레임 상세 상태 코드입니다.
	private String chgDtlStatCd;
	// 클레임 상세 상태명입니다.
	private String chgDtlStatNm;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 클레임 사유 코드입니다.
	private String chgReasonCd;
	// 클레임 사유명입니다.
	private String chgReasonNm;
	// 클레임 사유 상세입니다.
	private String chgReasonDtl;
	// 상품명입니다.
	private String goodsNm;
	// 클레임 수량입니다.
	private Integer qty;
	// 판매가(개당, ADD_AMT 포함) 금액입니다.
	private Integer saleAmt;
	// 상품쿠폰 환불 금액입니다.
	private Integer goodsCpnDcAmt;
	// 장바구니쿠폰 환불 금액입니다.
	private Integer cartCpnDcAmt;
	// 포인트 환불 금액입니다.
	private Integer pointDcAmt;
	// 환불 예정 금액입니다.
	private Integer expectedRefundAmt;
}
