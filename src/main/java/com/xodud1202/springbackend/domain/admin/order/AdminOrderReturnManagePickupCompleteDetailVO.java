package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 반품 회수완료 검수 팝업의 상품 상세 행 정보를 전달합니다.
public class AdminOrderReturnManagePickupCompleteDetailVO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 사이즈코드입니다.
	private String sizeId;
	// 반품수량입니다.
	private Integer qty;
	// 판매가입니다.
	private Integer saleAmt;
	// 추가금액입니다.
	private Integer addAmt;
	// 공급가입니다.
	private Integer supplyAmt;
	// 상품쿠폰 차감 금액입니다.
	private Integer goodsCouponDiscountAmt;
	// 장바구니쿠폰 차감 금액입니다.
	private Integer cartCouponDiscountAmt;
	// 포인트 환급 예정 금액입니다.
	private Integer pointDcAmt;
	// 저장된 반품 사유 코드입니다.
	private String chgReasonCd;
	// 저장된 반품 사유 상세입니다.
	private String chgReasonDtl;
	// 반품 상세 상태 코드입니다.
	private String chgDtlStatCd;
}
