package com.xodud1202.springbackend.domain.shop.auth;

import java.time.LocalDateTime;

// 고객 포인트 마스터 저장 명령을 전달합니다.
public record ShopCustomerPointSavePO(
	Long custNo,
	String pntGiveGbCd,
	String pntGiveMemo,
	Integer saveAmt,
	String ordNo,
	LocalDateTime expireDt,
	Long regNo,
	Long udtNo
) {
}
