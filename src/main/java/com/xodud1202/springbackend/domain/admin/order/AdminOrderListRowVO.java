package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 목록 행 정보를 정의합니다.
public class AdminOrderListRowVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 주문일시입니다.
	private String orderDt;
	// 주문상세 상태 코드입니다.
	private String ordDtlStatCd;
	// 주문상세 상태명입니다.
	private String ordDtlStatNm;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈코드입니다.
	private String sizeId;
	// 행 총 공급가 금액입니다.
	private Integer supplyAmt;
	// 행 총 판매가 금액입니다.
	private Integer saleAmt;
	// 행 총 실결제가 금액입니다.
	private Integer finalPayAmt;
	// 상품쿠폰 할인 금액입니다.
	private Integer goodsCouponDiscountAmt;
	// 장바구니쿠폰 할인 금액입니다.
	private Integer cartCouponDiscountAmt;
	// 포인트 사용 금액입니다.
	private Integer pointUseAmt;
	// 배송비 금액입니다.
	private Integer deliveryFeeAmt;
}
