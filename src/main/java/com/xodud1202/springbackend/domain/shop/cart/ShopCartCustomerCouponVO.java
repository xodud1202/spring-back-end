package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// 장바구니 쿠폰 예상 할인 계산용 고객 보유 쿠폰 정보를 전달합니다.
public class ShopCartCustomerCouponVO {
	// 고객 보유 쿠폰 번호입니다.
	private Long custCpnNo;
	// 고객 번호입니다.
	private Long custNo;
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 상태 코드입니다.
	private String cpnStatCd;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 타겟 코드입니다.
	private String cpnTargetCd;
	// 쿠폰 할인 구분 코드입니다.
	private String cpnDcGbCd;
	// 쿠폰 할인 값입니다.
	private Integer cpnDcVal;
	// 사용 가능 시작 일시입니다.
	private LocalDateTime cpnUsableStartDt;
	// 사용 가능 종료 일시입니다.
	private LocalDateTime cpnUsableEndDt;
}
