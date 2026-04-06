package com.xodud1202.springbackend.domain.shop.auth;

// 고객 포인트 상세 저장 명령을 전달합니다.
public record ShopCustomerPointDetailSavePO(
	Long pntNo,
	Integer pntAmt,
	String ordNo,
	String bigo,
	Long regNo
) {
}
