package com.xodud1202.springbackend.domain.shop.auth;

// 고객 포인트 마스터 저장 명령을 전달합니다.
public record ShopCustomerPointSavePO(
	Long custNo,
	String pntGiveGbCd,
	String pntGiveMemo,
	Integer saveAmt,
	Long regNo,
	Long udtNo
) {
}
