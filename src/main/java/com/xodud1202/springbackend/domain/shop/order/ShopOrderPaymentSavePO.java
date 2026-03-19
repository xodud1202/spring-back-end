package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// PAYMENT 저장 파라미터를 전달합니다.
public class ShopOrderPaymentSavePO {
	// 생성된 결제번호입니다.
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
	// 결제금액입니다.
	private Long payAmt;
	// 결제 디바이스 코드입니다.
	private String deviceGbCd;
	// PG 요청 원본 JSON 문자열입니다.
	private String reqRawJson;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
