package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 주문 결제 준비 요청 데이터를 전달합니다.
public class ShopOrderPaymentPreparePO {
	// 주문 진입 출처입니다.
	private String from;
	// 상품상세 재진입용 상품코드입니다.
	private String goodsId;
	// 결제 대상 장바구니 번호 목록입니다.
	private List<Long> cartIdList;
	// 선택 배송지명입니다.
	private String addressNm;
	// 선택 할인 상태입니다.
	private ShopOrderDiscountSelectionVO discountSelection;
	// 사용 포인트 금액입니다.
	private Integer pointUseAmt;
	// 결제수단 코드입니다.
	private String paymentMethodCd;
	// 환불 은행코드입니다.
	private String refundBankCd;
	// 환불 계좌번호입니다.
	private String refundBankNo;
	// 환불 예금주명입니다.
	private String refundHolderNm;
}
