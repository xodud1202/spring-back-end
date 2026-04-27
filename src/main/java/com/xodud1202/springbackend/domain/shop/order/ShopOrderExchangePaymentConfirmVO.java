package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 배송비 결제 승인 결과를 전달합니다.
public class ShopOrderExchangePaymentConfirmVO {
	// 클레임번호입니다.
	private String clmNo;
	// 원 주문번호입니다.
	private String ordNo;
	// 결제번호입니다.
	private Long payNo;
	// 결제수단 코드입니다.
	private String payMethodCd;
	// 결제상태 코드입니다.
	private String payStatCd;
	// 교환 상세 상태 코드입니다.
	private String chgDtlStatCd;
	// 주문명입니다.
	private String orderName;
	// 승인 또는 발급 금액입니다.
	private Long amount;
	// 가상계좌 은행코드입니다.
	private String bankCd;
	// 가상계좌 은행명입니다.
	private String bankNm;
	// 가상계좌 번호입니다.
	private String bankNo;
	// 가상계좌 예금주명입니다.
	private String vactHolderNm;
	// 가상계좌 입금기한입니다.
	private String vactDueDt;
}
