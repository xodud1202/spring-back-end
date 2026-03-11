package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 고객 포인트 마스터 저장 파라미터를 전달합니다.
public class ShopCustomerPointSavePO {
	// 생성된 포인트 번호입니다.
	private Long pntNo;
	// 고객 번호입니다.
	private Long custNo;
	// 포인트 지급 구분 코드입니다.
	private String pntGiveGbCd;
	// 포인트 지급 메모입니다.
	private String pntGiveMemo;
	// 지급 포인트 금액입니다.
	private Integer saveAmt;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
