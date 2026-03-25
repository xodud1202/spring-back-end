package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 취소내역 상품 상세 항목을 전달합니다.
public class ShopMypageCancelHistoryDetailVO {
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
	// 취소 수량입니다.
	private Integer qty;
	// 판매가입니다.
	private Integer saleAmt;
	// 추가 금액입니다.
	private Integer addAmt;
	// 취소 사유 코드입니다.
	private String chgReasonCd;
	// 취소 사유명입니다.
	private String chgReasonNm;
	// 취소 사유 상세입니다.
	private String chgReasonDtl;
	// 이미지 경로입니다.
	private String imgPath;
	// 이미지 URL입니다.
	private String imgUrl;
}
