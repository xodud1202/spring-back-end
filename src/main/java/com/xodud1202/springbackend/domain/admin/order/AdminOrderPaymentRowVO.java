package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 상세 하단 결제 행 정보를 정의합니다.
public class AdminOrderPaymentRowVO {
	// 주문번호입니다.
	private String ordNo;
	// 클레임번호입니다.
	private String clmNo;
	// 결제상태 코드입니다.
	private String payStatCd;
	// 결제상태명입니다.
	private String payStatNm;
	// 결제수단 코드입니다.
	private String payMethodCd;
	// 결제수단명입니다.
	private String payMethodNm;
	// 결제 금액입니다.
	private Long payAmt;
	// 거래번호입니다.
	private String tradeNo;
	// 결과 코드입니다.
	private String rspCode;
	// 결과 메시지입니다.
	private String rspMsg;
	// 무통장입금 계좌 은행코드입니다.
	private String bankCd;
	// 무통장입금 계좌 은행명입니다.
	private String bankNm;
	// 무통장입금 계좌번호입니다.
	private String bankNo;
	// 환불 은행코드입니다.
	private String refundBankCd;
	// 환불 계좌번호입니다.
	private String refundBankNo;
	// 처리일시입니다.
	private String processDt;
}
