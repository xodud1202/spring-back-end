package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// 고객 쿠폰 지급 저장 파라미터를 전달합니다.
public class ShopCustomerCouponSavePO {
	// 고객 번호입니다.
	private Long custNo;
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰 사용 가능 시작 일시입니다.
	private LocalDateTime cpnUsableStartDt;
	// 쿠폰 사용 가능 종료 일시입니다.
	private LocalDateTime cpnUsableEndDt;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
