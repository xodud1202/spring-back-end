package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// ORDER_DETAIL 저장 파라미터를 전달합니다.
public class ShopOrderDetailSavePO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 주문구분 코드입니다.
	private String ordGbCd;
	// 주문상세상태 코드입니다.
	private String ordDtlStatCd;
	// 고객번호입니다.
	private Long custNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 정상가입니다.
	private Integer supplyAmt;
	// 판매가입니다.
	private Integer saleAmt;
	// 추가금액입니다.
	private Integer addAmt;
	// 주문수량입니다.
	private Integer ordQty;
	// 취소수량입니다.
	private Integer cncQty;
	// 잔여수량입니다.
	private Integer rmnQty;
	// 상품쿠폰 고객쿠폰번호입니다.
	private Long goodsCpnNo;
	// 상품쿠폰 할인금액입니다.
	private Integer goodsCpnDcAmt;
	// 장바구니쿠폰 고객쿠폰번호입니다.
	private Long cartCpnNo;
	// 장바구니쿠폰 할인금액입니다.
	private Integer cartCpnDcAmt;
	// 포인트 사용금액입니다.
	private Integer pointUseAmt;
	// 적립 예정 포인트입니다.
	private Integer pointSaveAmt;
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
