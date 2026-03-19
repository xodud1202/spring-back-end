package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문 결제 승인 완료 응답 데이터를 전달합니다.
public class ShopOrderPaymentConfirmVO {
	// 주문번호입니다.
	private String ordNo;
	// 결제번호입니다.
	private Long payNo;
	// 결제수단 코드입니다.
	private String payMethodCd;
	// 결제상태 코드입니다.
	private String payStatCd;
	// 주문상태 코드입니다.
	private String ordStatCd;
	// 주문명입니다.
	private String orderName;
	// 승인 금액입니다.
	private Long amount;
	// 가상계좌 은행코드입니다.
	private String bankCd;
	// 가상계좌 번호입니다.
	private String bankNo;
	// 가상계좌 예금주명입니다.
	private String vactHolderNm;
	// 가상계좌 입금기한입니다.
	private String vactDueDt;
}
