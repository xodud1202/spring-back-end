package com.xodud1202.springbackend.service.order.support;

/**
 * @param startDate            조회 시작일입니다.
 * @param endDate              조회 종료일입니다.
 * @param startDateTime        조회 시작일시입니다.
 * @param endExclusiveDateTime 조회 종료일 다음날 00시 기준 비교 일시입니다.
 */ // 마이페이지 주문내역 조회 기간 보정 결과를 전달합니다.
public record ShopMypageOrderDateRange(
	String startDate,
	String endDate,
	String startDateTime,
	String endExclusiveDateTime
) {
	// 마이페이지 주문내역 조회 기간 보정 결과를 생성합니다.
	public ShopMypageOrderDateRange {
	}

	// 조회 시작일을 반환합니다.
	public String getStartDate() {
		return startDate;
	}

	// 조회 종료일을 반환합니다.
	public String getEndDate() {
		return endDate;
	}

	// 조회 시작일시를 반환합니다.
	public String getStartDateTime() {
		return startDateTime;
	}

	// 조회 종료일 다음날 00시 비교 일시를 반환합니다.
	public String getEndExclusiveDateTime() {
		return endExclusiveDateTime;
	}
}

