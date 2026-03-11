package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// 고객 발급용 쿠폰 유효기간 규칙 정보를 전달합니다.
public class ShopCouponIssueRuleVO {
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰 상태 코드입니다.
	private String cpnStatCd;
	// 쿠폰 사용 기간 구분 코드입니다.
	private String cpnUseDtGb;
	// 다운로드 후 사용 가능 일수입니다.
	private Integer cpnUsableDt;
	// 쿠폰 사용 시작 일시입니다.
	private LocalDateTime cpnUseStartDt;
	// 쿠폰 사용 종료 일시입니다.
	private LocalDateTime cpnUseEndDt;
}
