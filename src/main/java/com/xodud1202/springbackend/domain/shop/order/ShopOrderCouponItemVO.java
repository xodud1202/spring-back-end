package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// 주문서 쿠폰 선택 항목 정보를 전달합니다.
public class ShopOrderCouponItemVO {
	// 고객 보유 쿠폰 번호입니다.
	private Long custCpnNo;
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 적용 대상 코드입니다.
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
