package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문내역의 주문상세 행 정보를 전달합니다.
public class ShopMypageOrderDetailItemVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 주문상세 상태코드입니다.
	private String ordDtlStatCd;
	// 주문상세 상태명입니다.
	private String ordDtlStatNm;
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 사이즈 코드입니다.
	private String sizeId;
	// 주문 수량입니다.
	private Integer ordQty;
	// 판매가입니다.
	private Integer saleAmt;
	// 추가금액입니다.
	private Integer addAmt;
	// 상품 이미지 경로입니다.
	private String imgPath;
	// 상품 이미지 전체 URL입니다.
	private String imgUrl;
}
