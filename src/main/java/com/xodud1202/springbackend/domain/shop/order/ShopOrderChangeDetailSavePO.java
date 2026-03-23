package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문변경 상세 저장 파라미터를 전달합니다.
public class ShopOrderChangeDetailSavePO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 변경상세 구분 코드입니다.
	private String chgDtlGbCd;
	// 변경상세 상태 코드입니다.
	private String chgDtlStatCd;
	// 변경 사유 코드입니다.
	private String chgReasonCd;
	// 변경 사유 상세입니다.
	private String chgReasonDtl;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈 코드입니다.
	private String sizeId;
	// 변경 수량입니다.
	private Integer qty;
	// 추가금액 단가입니다.
	private Integer addAmt;
	// 변경 주문상세번호입니다.
	private Integer changeOrdDtlNo;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
