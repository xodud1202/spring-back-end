package com.xodud1202.springbackend.domain.shop.auth;

import java.time.LocalDateTime;

// 고객 쿠폰 지급 저장 명령을 전달합니다.
public record ShopCustomerCouponSavePO(
	Long custNo,
	Long cpnNo,
	LocalDateTime cpnUsableStartDt,
	LocalDateTime cpnUsableEndDt,
	Long regNo,
	Long udtNo
) {
}
