package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문취소 처리용 주문 마스터 정보를 전달합니다.
public class ShopOrderCancelOrderBaseVO {
	// 주문번호입니다.
	private String ordNo;
	// 고객번호입니다.
	private Long custNo;
	// 주문상태 코드입니다.
	private String ordStatCd;
	// 주문 일시입니다.
	private String orderDt;
	// 주문 확정 일시입니다.
	private String orderConfirmDt;
	// 배송비 쿠폰 고객쿠폰번호입니다.
	private Long delvCpnNo;
	// 배송비 쿠폰 할인 금액입니다.
	private Integer delvCpnDcAmt;
	// 배송비 쿠폰 적용 전 기본 배송비입니다.
	private Integer ordDelvAmt;
	// 환불 은행코드입니다.
	private String refundBankCd;
	// 환불 계좌번호입니다.
	private String refundBankNo;
	// 환불 예금주명입니다.
	private String refundHolderNm;
	// 디바이스 구분 코드입니다.
	private String deviceGbCd;
}
