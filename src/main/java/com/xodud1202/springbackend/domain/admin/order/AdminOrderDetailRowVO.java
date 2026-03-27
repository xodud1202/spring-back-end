package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 상세 행 정보를 정의합니다.
public class AdminOrderDetailRowVO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 주문수량입니다.
	private Integer ordQty;
	// 취소수량입니다.
	private Integer cncQty;
	// 잔여수량입니다.
	private Integer rmnQty;
	// 공급가 금액입니다.
	private Integer supplyAmt;
	// 판매가(개당, ADD_AMT 포함) 금액입니다.
	private Integer saleAmt;
	// 상품쿠폰 할인 금액입니다.
	private Integer goodsCpnDcAmt;
	// 장바구니쿠폰 할인 금액입니다.
	private Integer cartCpnDcAmt;
	// 포인트 사용 금액입니다.
	private Integer pointUseAmt;
	// 실결제금액입니다. (판매가 * 잔여수량 - 상품쿠폰할인 - 장바구니쿠폰할인 - 포인트할인)
	private Integer finalPayAmt;
}
