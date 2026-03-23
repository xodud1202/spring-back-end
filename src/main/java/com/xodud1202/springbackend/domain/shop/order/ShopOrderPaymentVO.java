package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// PAYMENT 조회 결과를 전달합니다.
public class ShopOrderPaymentVO {
	// 결제번호입니다.
	private Long payNo;
	// 주문번호입니다.
	private String ordNo;
	// 고객번호입니다.
	private Long custNo;
	// 결제상태 코드입니다.
	private String payStatCd;
	// 결제구분 코드입니다.
	private String payGbCd;
	// 결제수단 코드입니다.
	private String payMethodCd;
	// 주문구분 코드입니다.
	private String ordGbCd;
	// PG 구분 코드입니다.
	private String pgGbCd;
	// 원결제번호입니다.
	private Long orgPayNo;
	// 클레임번호입니다.
	private String clmNo;
	// 결제금액입니다.
	private Long payAmt;
	// 승인금액입니다.
	private Long aprvAmt;
	// 취소금액입니다.
	private Long cnlAmt;
	// Toss 결제키입니다.
	private String tossPaymentKey;
	// Toss 결제키 해시입니다.
	private String tossPaymentKeyHash;
	// 거래번호입니다.
	private String tradeNo;
	// 승인번호입니다.
	private String apprNo;
	// 응답코드입니다.
	private String rspCode;
	// 응답메시지입니다.
	private String rspMsg;
	// 은행코드입니다.
	private String bankCd;
	// 계좌번호입니다.
	private String bankNo;
	// 예금주명입니다.
	private String vactHolderNm;
	// 입금기한입니다.
	private String vactDueDt;
	// 카드사 코드입니다.
	private String cardCd;
	// 카드번호입니다.
	private String cardNo;
	// 디바이스 코드입니다.
	private String deviceGbCd;
	// 요청일시입니다.
	private String reqDt;
	// 승인일시입니다.
	private String apprDt;
	// 웹훅 반영일시입니다.
	private String webhookDt;
	// 요청 원본 JSON입니다.
	private String reqRawJson;
	// 응답 원본 JSON입니다.
	private String rspRawJson;
	// 웹훅 원본 JSON입니다.
	private String webhookRawJson;
}
