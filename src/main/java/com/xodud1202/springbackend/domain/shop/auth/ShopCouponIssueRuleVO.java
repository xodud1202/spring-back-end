package com.xodud1202.springbackend.domain.shop.auth;

import java.time.LocalDateTime;

// 고객 발급용 쿠폰 유효기간 규칙 정보를 전달합니다.
public record ShopCouponIssueRuleVO(
	Long cpnNo,
	String cpnStatCd,
	String cpnUseDtGb,
	Integer cpnUsableDt,
	LocalDateTime cpnUseStartDt,
	LocalDateTime cpnUseEndDt
) {
}
