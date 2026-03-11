package com.xodud1202.springbackend.domain.admin.coupon;

import lombok.Data;

@Data
// 관리자 쿠폰 대상 행 정보를 정의합니다.
public class CouponTargetRowVO {
	// 대상 구분 코드입니다.
	private String targetGbCd;
	// 대상 값입니다.
	private String targetValue;
	// 대상 표시명입니다.
	private String targetNm;
	// 브랜드 번호입니다.
	private Integer brandNo;
	// 브랜드명입니다.
	private String brandNm;
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// ERP 품번코드입니다.
	private String erpStyleCd;
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 기획전명입니다.
	private String exhibitionNm;
	// 카테고리 ID입니다.
	private String categoryId;
	// 카테고리명입니다.
	private String categoryNm;
	// 카테고리 레벨입니다.
	private Integer categoryLevel;
}
