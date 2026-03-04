package com.xodud1202.springbackend.domain.admin.coupon;

import lombok.Data;

@Data
// 관리자 쿠폰 목록 행 정보를 정의합니다.
public class CouponVO {
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 상태명입니다.
	private String cpnStatNm;
	// 쿠폰 종류명입니다.
	private String cpnGbNm;
	// 쿠폰 타겟명입니다.
	private String cpnTargetNm;
	// 다운로드 시작일시(YYYY-MM-DD HH24)입니다.
	private String cpnDownStartDt;
	// 다운로드 종료일시(YYYY-MM-DD HH24)입니다.
	private String cpnDownEndDt;
	// 고객 다운로드 가능 여부입니다.
	private String cpnDownAbleYn;
	// 상태 중지 일시(YYYY-MM-DD HH24)입니다.
	private String statStopDt;
}
