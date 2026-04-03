package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품내역 상품 상세 항목을 전달합니다.
public class ShopMypageReturnHistoryDetailVO {
	// 클레임 번호입니다.
	private String clmNo;
	// 주문상세 번호입니다.
	private Integer ordDtlNo;
	// 상품 ID입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 사이즈 ID입니다.
	private String sizeId;
	// 반품 수량입니다.
	private Integer qty;
	// 판매가입니다.
	private Integer saleAmt;
	// 추가 금액입니다.
	private Integer addAmt;
	// 공급가입니다.
	private Integer supplyAmt;
	// 상품쿠폰 환급 금액입니다.
	private Integer goodsCouponDiscountAmt;
	// 장바구니쿠폰 환급 금액입니다.
	private Integer cartCouponDiscountAmt;
	// 포인트 환급 금액입니다.
	private Integer pointDcAmt;
	// 반품 상세 상태 코드입니다.
	private String chgDtlStatCd;
	// 반품 상세 상태명입니다.
	private String chgDtlStatNm;
	// 반품 사유 코드입니다.
	private String chgReasonCd;
	// 반품 사유명입니다.
	private String chgReasonNm;
	// 반품 사유 상세입니다.
	private String chgReasonDtl;
	// 이미지 경로입니다.
	private String imgPath;
	// 이미지 URL입니다.
	private String imgUrl;
}
