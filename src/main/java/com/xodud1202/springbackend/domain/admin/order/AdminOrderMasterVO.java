package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 상세 조회 시 주문 마스터 정보를 정의합니다.
public class AdminOrderMasterVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문일시입니다.
	private String orderDt;
	// 결제일시입니다.
	private String orderConfirmDt;
	// 주문 고객명(주문자명)입니다.
	private String custNm;
	// 고객 휴대폰번호(받는사람 휴대폰번호 대체)입니다.
	private String custPhoneNumber;
	// 고객 이메일(받는사람 이메일 대체)입니다.
	private String custEmail;
	// 받는사람명입니다.
	private String rcvNm;
	// 받는사람 우편번호입니다.
	private String rcvPostNo;
	// 받는사람 배송 주소 베이스입니다.
	private String rcvAddrBase;
	// 받는사람 배송 상세 주소입니다.
	private String rcvAddrDtl;
	// 배송비 금액입니다.
	private Integer ordDelvAmt;
	// 배송비 쿠폰 할인 금액입니다.
	private Integer delvCpnDcAmt;
}
