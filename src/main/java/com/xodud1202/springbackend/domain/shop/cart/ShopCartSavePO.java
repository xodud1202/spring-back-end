package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 쇼핑몰 장바구니 등록 정보를 전달합니다.
public class ShopCartSavePO {
	// 생성된 장바구니 번호입니다.
	private Long cartId;
	// 장바구니 구분 코드입니다.
	private String cartGbCd;
	// 고객번호입니다.
	private Long custNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈 코드입니다.
	private String sizeId;
	// 수량입니다.
	private Integer qty;
	// 등록 기획전 번호입니다.
	private Integer exhibitionNo;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
