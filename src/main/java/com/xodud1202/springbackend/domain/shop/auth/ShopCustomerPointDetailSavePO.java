package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 고객 포인트 상세 저장 파라미터를 전달합니다.
public class ShopCustomerPointDetailSavePO {
	// 포인트 번호입니다.
	private Long pntNo;
	// 포인트 금액입니다.
	private Integer pntAmt;
	// 포인트 상세 비고입니다.
	private String bigo;
	// 등록자 번호입니다.
	private Long regNo;
}
