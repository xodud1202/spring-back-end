package com.xodud1202.springbackend.domain.admin.coupon;

import lombok.Data;

@Data
// 관리자 쿠폰 대상 저장 행 정보를 정의합니다.
public class CouponTargetSaveRowPO {
	// 대상 구분 코드입니다.
	private String targetGbCd;
	// 대상 값입니다.
	private String targetValue;
}
